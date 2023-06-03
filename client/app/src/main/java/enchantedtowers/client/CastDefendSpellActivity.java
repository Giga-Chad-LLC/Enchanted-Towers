package enchantedtowers.client;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import enchantedtowers.client.components.utils.ClientUtils;

public class CastDefendSpellActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_CAMERA = 1;

    private ImageView imageView;
    private Uri capturedImageUri;

    private static final Logger logger = Logger.getLogger(CastDefendSpellActivity.class.getName());

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::cameraLaunchCallback);

    private void cameraLaunchCallback(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK) {
            ClientUtils.showToastOnUIThread(CastDefendSpellActivity.this, "Error while taking image!", Toast.LENGTH_LONG);
            logger.warning("Error while taking picture, result code is not OK: code=" + result.getResultCode());
            return;
        }

        // The photo was taken successfully
        logger.info("Loading captured image bitmap in launcher, uri=" + capturedImageUri);
        Bitmap capturedImage = null;

        try {
            Bitmap savedOnDeviceImage = MediaStore.Images.Media.getBitmap(getContentResolver(), capturedImageUri);
            capturedImage = rotateImageIfRequired(getBaseContext(), savedOnDeviceImage, capturedImageUri);
        } catch (IOException e) {
            ClientUtils.showToastOnUIThread(CastDefendSpellActivity.this, "Unable to show create image", Toast.LENGTH_LONG);
            logger.warning("Error while loading saved image: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Create opencv 'Mat' object for taken photo
        Mat imageMat = new Mat();
        Utils.bitmapToMat(capturedImage, imageMat);

        // Convert to greyscale
        Mat grayscaleImageMat = new Mat();
        Imgproc.cvtColor(imageMat, grayscaleImageMat, Imgproc.COLOR_RGB2GRAY);
        imageMat.release();

        // Apply histogram equalization or adaptive histogram equalization
//        Mat highContrastImageMat = new Mat();
//        Imgproc.equalizeHist(grayscaleImageMat, highContrastImageMat);
//        grayscaleImageMat.release();

        // Apply contrast-limited adaptive histogram equalization (CLAHE) for more localized enhancement
//        Mat highContrastImageMat = new Mat();
//        Imgproc.createCLAHE(1.0, new Size(50, 50)).apply(grayscaleImageMat, highContrastImageMat);
//        grayscaleImageMat.release();

        // retrieve edges of the image
        Mat edgesImageMat = new Mat();
        // TODO: learn how to setup good thresholds
        double highThreshold = 180.0;
        double lowThreshold = 0.6 * highThreshold;
        Imgproc.Canny(grayscaleImageMat, edgesImageMat, lowThreshold, highThreshold);
        grayscaleImageMat.release();

        // Run morphological operations
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        Mat smallKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2, 2));
        Imgproc.morphologyEx(edgesImageMat, edgesImageMat, Imgproc.MORPH_CLOSE, kernel);
        Imgproc.morphologyEx(edgesImageMat, edgesImageMat, Imgproc.MORPH_OPEN, smallKernel);
        // Imgproc.morphologyEx(edgesImageMat, edgesImageMat, Imgproc.MORPH_OPEN, kernel);

        // get contours from the image
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        // TODO: learn about different types of modes and algorithms of finding contours
        Imgproc.findContours(edgesImageMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        hierarchy.release(); // don't need it yet, I guess

        // Show found contours on the image
        Bitmap edgesBitmap = Bitmap.createBitmap(capturedImage.getWidth(), capturedImage.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(edgesImageMat, edgesBitmap);
        Bitmap resultingBitmap = blitContoursToBitmap(edgesBitmap, contours);

        edgesImageMat.release();

        imageView.setImageBitmap(resultingBitmap);

        // remove temporary image file
        logger.info("Remove created temp image from filesystem");
        deleteTempImageFile(capturedImageUri);
        capturedImageUri = null;
    }

    private Bitmap blitContoursToBitmap(Bitmap targetBitmap, List<MatOfPoint> contours) {
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
        for (MatOfPoint contour : contours) {
            // Convert the contour to a list of points
            List<Point> opencvPoints = contour.toList();
            // logger.info("Path: length=" + opencvPoints.size() + ", points=" + opencvPoints);

            // Create a path and move to the first point
            Point firstPoint = opencvPoints.get(0);
            path.moveTo((float) firstPoint.x, (float) firstPoint.y);

            // Draw lines connecting the subsequent points
            for (int i = 1; i < opencvPoints.size(); i++) {
                Point point = opencvPoints.get(i);
                path.lineTo((float) point.x, (float) point.y);
            }

            // Close the contour if necessary
            if (contour.size().height > 2) {
                path.close();
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        imageView = findViewById(R.id.capturedImageView);
        Button captureButton = findViewById(R.id.captureImageButton);

        captureButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(
                    CastDefendSpellActivity.this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                // TODO: maybe can reuse permissions manager from map
                requestCameraPermission();
            }
        });
    }

    private void startCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            capturedImageUri = createTempImageFile();
            logger.info("Image URI: " + capturedImageUri);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
            cameraLauncher.launch(takePictureIntent);
            // deleteTempImageFile(imageUri);
        }
        else {
            ClientUtils.showToastOnUIThread(CastDefendSpellActivity.this, "Unable to start camera. Try restarting the game.", Toast.LENGTH_LONG);
        }
    }


    private Uri createTempImageFile() {
        File imageFile;

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "ENCHANTED_TOWERS_IMG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            imageFile = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );

            logger.info("Create tmp file for storing high quality image: name='" + imageFile.getName() + "'");
            // required for escaping FileUriExposedException exception
            // when saving and removing image from storage without additional permissions
            return FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".fileprovider", imageFile);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void deleteTempImageFile(Uri imageUri) {
        int deletedObjects = getBaseContext().getContentResolver().delete(imageUri, null, null);
        if (deletedObjects != 1) {
            logger.warning("Something went wrong while deleting temp image, deleted objects count " + deletedObjects);
        }
        else {
            logger.info("Deleted temp image successfully: " + imageUri.getPath());
        }
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            // Show an explanation to the user
            // You can customize the message according to your needs
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_PERMISSION_CAMERA);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_PERMISSION_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                // Camera permission was denied
                // You can show a message or handle the denial scenario accordingly
                logger.warning("Camera permissions denied!");
                ClientUtils.showToastOnUIThread(CastDefendSpellActivity.this, "Cannot take a picture without camera permissions!", Toast.LENGTH_LONG);
            }
        }
    }

}