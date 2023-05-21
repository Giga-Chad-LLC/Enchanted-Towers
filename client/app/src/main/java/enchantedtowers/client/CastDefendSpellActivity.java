package enchantedtowers.client;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

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
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import enchantedtowers.client.components.utils.ClientUtils;

public class CastDefendSpellActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_CAMERA = 1;

    private ImageView imageView;
    private Uri capturedImageUri;

    private static final Logger logger = Logger.getLogger(CastDefendSpellActivity.class.getName());

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK) {
                    ClientUtils.showToastOnUIThread(CastDefendSpellActivity.this, "Error while taking image!", Toast.LENGTH_LONG);
                    logger.warning("Error while taking picture, result code is not OK: code=" + result.getResultCode());
                    return;
                }

                // The photo was taken successfully
                // You can access the photo using the 'result' parameter
                // Bitmap capturedImage = (Bitmap) data.getExtras().get("data");
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

                Mat imageMat = new Mat();
                Utils.bitmapToMat(capturedImage, imageMat);
                logger.info("Created opencv 'Mat' object for taken photo");

                Mat grayscaleImageMat = new Mat();
                Imgproc.cvtColor(imageMat, grayscaleImageMat, Imgproc.COLOR_RGB2GRAY); // COLOR_BGR2GRAY

                logger.info("Show taken image in grayscale");
                Utils.matToBitmap(grayscaleImageMat, capturedImage);
                imageView.setImageBitmap(capturedImage);

                imageMat.release();
                grayscaleImageMat.release();
                logger.info("Released opencv 'Mat' object for taken photo");
            });

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
        if (imageUri != null) {
            File imageFile = new File(imageUri.getPath());
            if (imageFile.exists()) {
                if (!imageFile.delete()) {
                    logger.warning("Unable to delete temp file from the gallery");
                }
            }
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