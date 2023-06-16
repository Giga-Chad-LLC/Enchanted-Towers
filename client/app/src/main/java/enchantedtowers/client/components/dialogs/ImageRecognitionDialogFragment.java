package enchantedtowers.client.components.dialogs;

import android.Manifest;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.snackbar.Snackbar;

import org.locationtech.jts.geom.Envelope;

import java.util.List;
import java.util.Objects;

import enchantedtowers.client.R;
import enchantedtowers.client.components.canvas.CanvasDefendSpellDecorator;
import enchantedtowers.client.components.canvas.CanvasWidget;
import enchantedtowers.client.components.permissions.PermissionManager;
import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.client.interactors.canvas.CanvasDrawStateInteractor;
import enchantedtowers.game_models.DefendSpell;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_models.utils.Vector2;

public class ImageRecognitionDialogFragment extends DialogFragment {
    private final String[] cameraPermission = new String[] { Manifest.permission.CAMERA };
    private ActivityResultLauncher<String[]> cameraPermissionLauncher;


    private int defendSpellId = -1;
    private String defendSpellName = "";

    public static ImageRecognitionDialogFragment newInstance() {
        return new ImageRecognitionDialogFragment();
    }

    public void setDefendSpellId(int defendSpellId) {
        this.defendSpellId = defendSpellId;
    }

    public void setDefendSpellName(String defendSpellName) {
        this.defendSpellName = defendSpellName;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Dialog root = getDialog();
                    if (root == null) {
                        return;
                    }
                    View frameLayout = root.findViewById(R.id.defend_spell_preview_layout);

                    boolean cameraPermissionGranted = Boolean.TRUE.equals(result.getOrDefault(
                            Manifest.permission.CAMERA, false));

                    if (cameraPermissionGranted) {
                        ClientUtils.showSnackbar(frameLayout, "Camera permissions granted. Cast a defend spell!", Snackbar.LENGTH_LONG);
                        // TODO: run smth
                    }
                    else {
                        ClientUtils.showSnackbar(frameLayout, "Content might be limited. Please, grant access of camera.", Snackbar.LENGTH_LONG);
                    }
                }
            );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_recognition_dialog, container, false);

        // remove default dialog styles
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // setting leave button
        Button leaveButton = view.findViewById(R.id.leave_photo_button);
        leaveButton.setOnClickListener(v -> dismiss());

        // Layouts
        LinearLayout submitPhotoLayout = view.findViewById(R.id.submit_photo_layout);
        LinearLayout takePhotoLayout = view.findViewById(R.id.take_photo_layout);

        // setting take photo button
        Button takePhotoButton = view.findViewById(R.id.take_photo_button);
        takePhotoButton.setOnClickListener(v -> {
            System.out.println("Take photo");
            takePhotoLayout.setVisibility(View.INVISIBLE);
            submitPhotoLayout.setVisibility(View.VISIBLE);
        });

        // setting retake photo button
        Button retakePhotoButton = view.findViewById(R.id.retake_photo_button);
        retakePhotoButton.setOnClickListener(v -> {
            System.out.println("Retake photo");
        });

        // setting submit photo
        Button submitPhotoButton = view.findViewById(R.id.submit_photo_button);
        submitPhotoButton.setOnClickListener(v -> {
            System.out.println("Submit photo");
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        requestCameraPermission();

        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }

        // Set defend spell name
        TextView selectedSpellTextView = dialog.findViewById(R.id.selected_spell_name);
        selectedSpellTextView.setText(defendSpellName);

        // Draw defend spell
        drawDefendSpell();

        // Adjust the dialog size and position if needed
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void requestCameraPermission() {
        new PermissionManager()
            .withPermissions(
                cameraPermission,
                requireContext(),
                () -> {
                    System.out.println("Camera permission already granted");
                }
            )
            .otherwise(() -> {
                System.out.println("Camera not permission already granted");

                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    showCameraRequestPermissionRationale(cameraPermissionLauncher);
                }
                else {
                    cameraPermissionLauncher.launch(cameraPermission);
                }
            });
    }

    private void showCameraRequestPermissionRationale(ActivityResultLauncher<String[]> cameraPermissionLauncher) {
        Runnable positiveCallback = () -> cameraPermissionLauncher.launch(cameraPermission);

        Runnable negativeCallback = () -> ClientUtils.showSnackbar(
            Objects.requireNonNull(getDialog()).findViewById(R.id.defend_spell_preview_layout),
            "Content might be limited",
            Snackbar.LENGTH_LONG
        );

        Dialog currentDialog = getDialog();
        if (currentDialog != null) {
            Dialog dialog = CameraRequestPermissionRationaleDialog.newInstance(currentDialog.getContext(), positiveCallback, negativeCallback);
            dialog.show();
        }
    }

    private void drawDefendSpell() {
        Dialog dialog = getDialog();
        DefendSpell defendSpell = SpellBook.getDefendSpellTemplateById(defendSpellId);

        if (dialog == null || defendSpell == null) {
            return;
        }

        CanvasWidget canvas = dialog.findViewById(R.id.canvas_widget);
        Envelope boundingBox = defendSpell.getBoundary();

        ViewGroup.LayoutParams layoutParams = canvas.getLayoutParams();
        float density = canvas.getContext().getResources().getDisplayMetrics().density;
        double padding_dp = 14 * density;

        int targetWidth = layoutParams.width;
        int targetHeight = layoutParams.height;

        double scaleX = (targetWidth - 2 * padding_dp) / boundingBox.getWidth();
        double scaleY = (targetHeight - 2 * padding_dp) / boundingBox.getHeight();

        double applyingScale = Math.min(scaleX, scaleY);

        DefendSpell scaledSpell = defendSpell.getScaledDefendSpell(applyingScale, applyingScale);
        Envelope scaledBoundingBox = scaledSpell.getBoundary();

        double offsetX = (targetWidth - scaledBoundingBox.getWidth()) / 2.0;
        double offsetY = (targetHeight - scaledBoundingBox.getHeight()) / 2.0;

        canvas.setInteractors(List.of(
                new CanvasDrawStateInteractor()
        ));
        canvas.getState().addItem(new CanvasDefendSpellDecorator(
                scaledSpell, new Vector2(offsetX, offsetY)
        ));
        canvas.postInvalidate();
    }
}
