package enchantedtowers.client.components.providers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Pair;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.logging.Logger;

import enchantedtowers.game_models.DefendSpell;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_models.utils.Vector2;
import kotlin.Triple;

public class ImageContoursProvider {
    private final FragmentActivity parentActivity;
    private static final Logger logger = Logger.getLogger(ImageContoursProvider.class.getName());
    private final ActivityResultLauncher<Intent> cameraLauncher;
    private Uri capturedImageUri;
    private final Consumer<String> onErrorCallback;

    public ImageContoursProvider(
            Fragment parentDialog,
            FragmentActivity parentActivity,
            Consumer<Pair<Bitmap, List<List<Vector2>>>> onSuccess,
            Consumer<String> onError
    ) {
        this.parentActivity = parentActivity;
        this.onErrorCallback = onError;

        // Use Fragment class here to register for activity result
        this.cameraLauncher = parentDialog.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            (result) -> {
                try {
                    Pair<Bitmap, List<List<Vector2>>> data = processImage(result);
                    onSuccess.accept(data);
                }
                catch(Exception e) {
                    onError.accept(e.getMessage());
                }
            }
        );
    }

    public void startCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(parentActivity.getPackageManager()) != null) {
            capturedImageUri = createTempImageFile();
            logger.info("Image URI: " + capturedImageUri);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
            cameraLauncher.launch(takePictureIntent);
        }
        else {
            onErrorCallback.accept("Unable to start camera, try restarting the game.");
        }
    }

    private Pair<Bitmap, List<List<Vector2>>> processImage(ActivityResult result) throws Exception {
        if (result.getResultCode() != Activity.RESULT_OK) {
            // ClientUtils.showToastOnUIThread(parentActivity, "Error while taking image!", Toast.LENGTH_LONG);
            logger.warning("Error while taking picture, result code: " + result.getResultCode());
            throw new Exception("Error while taking picture, result code: " + result.getResultCode());
        }

        // Loading image from temp file
        logger.info("Loading captured image bitmap in launcher, uri=" + capturedImageUri);
        Bitmap capturedImage;

        try {
            Bitmap savedOnDeviceImage = MediaStore.Images.Media.getBitmap(parentActivity.getContentResolver(), capturedImageUri);
            Bitmap rotatedImage = rotateImageIfRequired(parentActivity.getBaseContext(), savedOnDeviceImage, capturedImageUri);
            capturedImage = cropImageAroundBorders(rotatedImage, 60, 60);
        } catch (IOException e) {
            // ClientUtils.showToastOnUIThread(parentActivity, "Unable to show create image", Toast.LENGTH_LONG);
            logger.warning("Error while loading saved image: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Error while loading saved image: " + e.getMessage());
        }

        // Create opencv 'Mat' object for taken photo
        Mat imageMat = new Mat();
        Utils.bitmapToMat(capturedImage, imageMat);

        // Convert to greyscale
        Mat grayscaleImageMat = convertToGrayScale(imageMat);
        imageMat.release();

        // retrieve edges of the image
        double highThreshold = 180.0;
        double lowThreshold = 0.85 * highThreshold;
        Mat edgesImageMat = retrieveEdges(grayscaleImageMat, highThreshold, lowThreshold);
        grayscaleImageMat.release();

        // Filtering noises
        Mat blurredImage = blurImage(edgesImageMat, 3, 2.0, 0.0);
        edgesImageMat.release();

        // Run morphological operations
        applyMorphologyOperations(blurredImage, List.of(
                new Triple<>(Imgproc.MORPH_CLOSE, Imgproc.MORPH_RECT, new Size(5, 5)),
                new Triple<>(Imgproc.MORPH_OPEN, Imgproc.MORPH_ELLIPSE, new Size(2, 2))
        ));

        // get contours from the image
        List<List<Vector2>> contours = retrieveContours(blurredImage);

        // Show found contours on the image
        Bitmap edgesBitmap = Bitmap.createBitmap(capturedImage.getWidth(), capturedImage.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(blurredImage, edgesBitmap);
        blurredImage.release();

        Bitmap resultingBitmap = blitContoursToBitmap(
                edgesBitmap,
                enchantedtowers.game_models.utils.Utils.getNormalizedLines(contours)
        );
        // blit saved templates for debug purposes
        for (var entry : SpellBook.getDefendSpellsTemplates().entrySet()) {
            DefendSpell defendSpell = entry.getValue();
            resultingBitmap = blitContoursToBitmap(resultingBitmap, defendSpell.getPoints());
        }

        // remove temporary image file
        deleteTempImageFile(capturedImageUri);
        capturedImageUri = null;

        return new Pair<>(resultingBitmap, contours);
    }

    private Mat convertToGrayScale(Mat imageMat) {
        Mat grayscaleImageMat = new Mat();
        Imgproc.cvtColor(imageMat, grayscaleImageMat, Imgproc.COLOR_RGB2GRAY);
        return grayscaleImageMat;
    }

    private Mat retrieveEdges(Mat imageMat, double highThreshold, double lowThreshold) {
        // TODO: learn how to setup good thresholds
        Mat edgesImageMat = new Mat();
        Imgproc.Canny(imageMat, edgesImageMat, lowThreshold, highThreshold);
        return edgesImageMat;
    }

    private Mat blurImage(Mat imageMat, int kernelSize, double sigmaX, double sigmaY) {
        Mat blurredImage = new Mat();
        Imgproc.GaussianBlur(imageMat, blurredImage, new Size(kernelSize, kernelSize), sigmaX, sigmaY);
        return blurredImage;
    }

    private void applyMorphologyOperations(Mat imageMat, List<Triple<Integer, Integer, Size>> operations) {
        for (var operation : operations) {
            int operationType = operation.getFirst();
            int kernelStructure = operation.getSecond();
            Size kernelSize = operation.getThird();

            Mat kernel = Imgproc.getStructuringElement(kernelStructure, kernelSize);
            Imgproc.morphologyEx(imageMat, imageMat, operationType, kernel);
        }
    }

    private List<List<Vector2>> retrieveContours(Mat imageMat) {
        List<MatOfPoint> opencvContours = new ArrayList<>();
        Mat hierarchy = new Mat();
        // TODO: learn about different types of modes and algorithms of finding contours
        Imgproc.findContours(imageMat, opencvContours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        hierarchy.release(); // don't need it yet, I guess

        // cast contours to a better data structure
        return getContoursAsList(opencvContours);
    }

    /**
     * Crop image from the sides in order to minimize noises
     * @return cropped image bitmap
     */
    private Bitmap cropImageAroundBorders(Bitmap bitmap, int x, int y) {
        Mat imageMat = new Mat();
        Utils.bitmapToMat(bitmap, imageMat);

        // do the cropping
        int cropWidth = imageMat.width() - 2 * x;
        int cropHeight = imageMat.height() - 2 * y;
        Rect roi = new Rect(x, y, cropWidth, cropHeight);
        Mat croppedImageMat = new Mat(imageMat, roi);
        imageMat.release();

        // Convert the cropped Mat back to a Bitmap
        Bitmap croppedBitmap = Bitmap.createBitmap(croppedImageMat.cols(), croppedImageMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(croppedImageMat, croppedBitmap);
        croppedImageMat.release();

        return croppedBitmap;
    }

    private List<List<Vector2>> getContoursAsList(List<MatOfPoint> contours) {
        List<List<Vector2>> result = new ArrayList<>();

        for (MatOfPoint contour : contours) {
            List<Point> opencvPoints = contour.toList();
            if (opencvPoints.isEmpty()) {
                continue;
            }

            List<Vector2> line = new ArrayList<>();

            for (Point p : opencvPoints) {
                line.add(new Vector2(p.x, p.y));
            }

            // Close the contour if necessary
            if (contour.size().height > 2) {
                line.add(new Vector2(line.get(0).x, line.get(0).y));
            }

            result.add(line);
        }

        return result;
    }

    private Bitmap blitContoursToBitmap(Bitmap targetBitmap, List<List<Vector2>> contours) {
        // Create a new canvas from the target bitmap
        Bitmap mutableBitmap = targetBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        // Create a paint object for drawing the contours
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(5.0f);

        // Iterate over the contours and draw them on the canvas
        Path path = new Path();
        for (List<Vector2> contour : contours) {
            // Create a path and move to the first point
            Vector2 firstPoint = contour.get(0);
            path.moveTo((float) firstPoint.x, (float) firstPoint.y);

            // Draw lines connecting the subsequent points
            for (int i = 1; i < contour.size(); i++) {
                Vector2 point = contour.get(i);
                path.lineTo((float) point.x, (float) point.y);
            }

            // Draw the contour on the canvas
            canvas.drawPath(path, paint);
            path.reset();
        }

        return mutableBitmap;
    }

    /**
     * On some devices retrieved image is captured rotated, so we need to adjust to that
     * and rotate the bitmap that we will show on the screen.
     * @param context to get the correct content resolver
     * @param img initial bitmap, without rotation applied
     * @param selectedImage uri where the image is saved
     * @return rotated new bitmap (allocated additional memory)
     */
    private static Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(selectedImage);
        ExifInterface ei;
        if (Build.VERSION.SDK_INT > 23) {
            ei = new ExifInterface(input);
        }
        else {
            ei = new ExifInterface(selectedImage.getPath());
        }

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        return switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270);
            default -> img;
        };
    }

    /**
     * Rotates given image (as a bitmap) by angle in degrees counter-clockwise
     * @return new bitmap with rotated image
     */
    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_camera);
//
//        imageView = findViewById(R.id.capturedImageView);
//        Button captureButton = findViewById(R.id.captureImageButton);
//
//        captureButton.setOnClickListener(v -> {
//            if (ContextCompat.checkSelfPermission(
//                    CastDefendSpellActivity.this, Manifest.permission.CAMERA) ==
//                    PackageManager.PERMISSION_GRANTED) {
//                startCamera();
//            } else {
//                // TODO: maybe can reuse permissions manager from map
//                requestCameraPermission();
//            }
//        });
//    }

    private Uri createTempImageFile() {
        File imageFile;

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "ENCHANTED_TOWERS_IMG_" + timeStamp + "_";
            File storageDir = parentActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            imageFile = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );

            logger.info("Create tmp file for storing high quality image: name='" + imageFile.getName() + "'");
            // required for escaping FileUriExposedException exception
            // when saving and removing image from storage without additional permissions
            return FileProvider.getUriForFile(parentActivity.getApplicationContext(), parentActivity.getPackageName() + ".fileprovider", imageFile);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void deleteTempImageFile(Uri imageUri) {
        int deletedObjects = parentActivity.getBaseContext().getContentResolver().delete(imageUri, null, null);
        if (deletedObjects != 1) {
            logger.warning("Something went wrong while deleting temp image, deleted objects count " + deletedObjects);
        }
        else {
            logger.info("Deleted temp image successfully: " + imageUri.getPath());
        }
    }

//    private void requestCameraPermission() {
//        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
//                Manifest.permission.CAMERA)) {
//            // Show an explanation to the user
//            // You can customize the message according to your needs
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.CAMERA},
//                    REQUEST_PERMISSION_CAMERA);
//        } else {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.CAMERA},
//                    REQUEST_PERMISSION_CAMERA);
//        }
//    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == REQUEST_PERMISSION_CAMERA) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                startCamera();
//            } else {
//                // Camera permission was denied
//                // You can show a message or handle the denial scenario accordingly
//                logger.warning("Camera permissions denied!");
//                ClientUtils.showToastOnUIThread(CastDefendSpellActivity.this, "Cannot take a picture without camera permissions!", Toast.LENGTH_LONG);
//            }
//        }
//    }
}