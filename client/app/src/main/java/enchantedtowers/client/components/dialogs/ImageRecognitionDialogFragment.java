package enchantedtowers.client.components.dialogs;

import android.Manifest;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import enchantedtowers.client.R;
import enchantedtowers.client.components.canvas.CanvasDefendSpellDecorator;
import enchantedtowers.client.components.canvas.CanvasWidget;
import enchantedtowers.client.components.permissions.PermissionManager;
import enchantedtowers.client.components.providers.ImageContoursProvider;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.client.interactors.canvas.CanvasDrawStateInteractor;
import enchantedtowers.common.utils.proto.common.PlayerData;
import enchantedtowers.common.utils.proto.requests.CastDefendSpellRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.services.TowerAttackServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import enchantedtowers.game_logic.algorithm.DefendSpellMatchingAlgorithm;
import enchantedtowers.game_models.DefendSpell;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_models.utils.Vector2;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class ImageRecognitionDialogFragment extends DialogFragment {
    private final String[] cameraPermission = new String[] { Manifest.permission.CAMERA };
    private ActivityResultLauncher<String[]> cameraPermissionLauncher;
    private int defendSpellId = -1;
    private boolean isMatchedWithTemplate = false;
    private String defendSpellName = "";
    private ImageContoursProvider contoursExtractor;
    private final TowerAttackServiceGrpc.TowerAttackServiceStub asyncStub;
    private final Logger logger = Logger.getLogger(ImageRecognitionDialogFragment.class.getName());
    private final DialogFragment parentDialog;

    public static ImageRecognitionDialogFragment newInstance(DialogFragment parentDialog) {
        return new ImageRecognitionDialogFragment(parentDialog);
    }

    public ImageRecognitionDialogFragment(DialogFragment parentDialog) {
        super();

        this.parentDialog = parentDialog;

        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();

        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        asyncStub = TowerAttackServiceGrpc.newStub(channel);
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
                        ClientUtils.showSnackbar(frameLayout, "Content might be limited. Please, grant access for camera.", Snackbar.LENGTH_LONG);
                    }
                }
            );

        contoursExtractor = new ImageContoursProvider(
                this,
                requireActivity(),
                this::onContoursExtractionSuccess,
                this::onContoursExtractionError
        );
    }

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

        // setting take photo button
        Button takePhotoButton = view.findViewById(R.id.take_photo_button);
        takePhotoButton.setOnClickListener(this::onCameraOpen);

        // setting retake photo button
        Button retakePhotoButton = view.findViewById(R.id.retake_photo_button);
        retakePhotoButton.setOnClickListener(this::onCameraOpen);

        // setting submit photo
        Button submitPhotoButton = view.findViewById(R.id.submit_photo_button);
        submitPhotoButton.setOnClickListener(this::onSubmit);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }

        // Set defend spell name
        TextView selectedSpellTextView = dialog.findViewById(R.id.selected_spell_name);
        if (selectedSpellTextView != null) {
            selectedSpellTextView.setText(defendSpellName);
        }

        // Draw defend spell
        drawDefendSpell();

        // Adjust the dialog size and position if needed
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    public void setDefendSpellId(int defendSpellId) {
        this.defendSpellId = defendSpellId;
    }

    public void setDefendSpellName(String defendSpellName) {
        this.defendSpellName = defendSpellName;
    }

    private void onCameraOpen(View v) {
        withCameraPermission(() -> {
            System.out.println("Camera permission already granted");
            contoursExtractor.startCamera();
        });
    }

    private void onSubmit(View v) {
        Dialog dialog = getDialog();

        if (dialog == null) {
            return;
        }

        if (!isMatchedWithTemplate) {
            ClientUtils.showSnackbar(
                    dialog.findViewById(R.id.defend_spell_preview_layout),
                    "Match percentage with defend spell is not enough!",
                    Snackbar.LENGTH_LONG
            );
            return;
        }

        ClientStorage storage = ClientStorage.getInstance();
        if (storage.getPlayerId().isEmpty() || storage.getTowerId().isEmpty()) {
            ClientUtils.showSnackbar(
                    dialog.findViewById(R.id.defend_spell_preview_layout),
                    "Player id and tower id must be set when casting defend spell!",
                    Snackbar.LENGTH_LONG
            );
            return;
        }

        int playerId = storage.getPlayerId().get();
        int towerId = storage.getTowerId().get();

        CastDefendSpellRequest request = CastDefendSpellRequest.newBuilder()
            .setPlayerData(
                PlayerData.newBuilder()
                    .setPlayerId(playerId)
                    .build()
            )
            .setTowerId(towerId)
            .setDefendSpellId(defendSpellId)
            .build();

        asyncStub
            .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
            .castDefendSpell(request, new StreamObserver<>() {
                private boolean hadError = false;

                @Override
                public void onNext(ActionResultResponse response) {
                    if (response.hasError()) {
                        this.hadError = true;
                        ClientUtils.showSnackbar(
                                dialog.findViewById(R.id.defend_spell_preview_layout),
                                "Error: " + response.getError().getMessage(),
                                Snackbar.LENGTH_LONG
                        );

                        logger.warning("onNext error: type=" + response.getError().getType() + ", message=" + response.getError().getMessage());
                    }
                }

                @Override
                public void onError(Throwable t) {
                    ClientUtils.showSnackbar(
                            dialog.findViewById(R.id.defend_spell_preview_layout),
                            t.getMessage(),
                            Snackbar.LENGTH_LONG
                    );

                    logger.warning("oError: message=" + t.getMessage());
                    t.printStackTrace();
                }

                @Override
                public void onCompleted() {
                    if (!hadError) {
                        parentDialog.dismiss();
                        ImageRecognitionDialogFragment.this.dismiss();
                    }
                }
            });
    }

    private void onContoursExtractionSuccess(Pair<Bitmap, List<List<Vector2>>> data) {
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        var processedImage = data.first;
        var contours = data.second;

        // Update layouts
        LinearLayout submitPhotoLayout = dialog.findViewById(R.id.submit_photo_layout);
        LinearLayout takePhotoLayout = dialog.findViewById(R.id.take_photo_layout);

        if (takePhotoLayout != null) {
            takePhotoLayout.setVisibility(View.INVISIBLE);
        }
        if (submitPhotoLayout != null) {
            submitPhotoLayout.setVisibility(View.VISIBLE);
        }

        // Show contours of the taken image
        ImageView imageView = dialog.findViewById(R.id.captured_photo_preview);
        if (imageView != null) {
            imageView.setImageBitmap(processedImage);
        }

        // run pattern matching
        double matchPercentage = DefendSpellMatchingAlgorithm.getTemplateMatchPercentageWithHausdorffMetric(defendSpellId, contours);
        String percent = Math.round(matchPercentage * 100) + "%";
        TextView defendSpellMatch = dialog.findViewById(R.id.cast_match_percentage);
        if (defendSpellMatch != null) {
            defendSpellMatch.setText(percent);
        }

        isMatchedWithTemplate = DefendSpellMatchingAlgorithm.isMatchedWithTemplate(matchPercentage);
    }

    private void onContoursExtractionError(String message) {
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        ClientUtils.showSnackbar(
                dialog.findViewById(R.id.defend_spell_preview_layout),
                "Error while processing image: " + message,
                Snackbar.LENGTH_LONG
        );
    }

    private void withCameraPermission(Runnable onSuccessCallback) {
        new PermissionManager()
            .withPermissions(
                cameraPermission,
                requireContext(),
                    onSuccessCallback::run
            )
            .otherwise(() -> {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    showCameraRequestPermissionRationale(cameraPermissionLauncher);
                }
                else {
                    cameraPermissionLauncher.launch(cameraPermission);
                }
            });
    }

    private void showCameraRequestPermissionRationale(ActivityResultLauncher<String[]> cameraPermissionLauncher) {
        Dialog currentDialog = getDialog();
        if (currentDialog == null) {
            return;
        }

        Runnable positiveCallback = () -> cameraPermissionLauncher.launch(cameraPermission);
        Runnable negativeCallback = () -> ClientUtils.showSnackbar(
            currentDialog.findViewById(R.id.defend_spell_preview_layout),
            "Content might be limited",
            Snackbar.LENGTH_LONG
        );

        Dialog dialog = CameraRequestPermissionRationaleDialog.newInstance(
            currentDialog.getContext(),
            positiveCallback,
            negativeCallback
        );
        dialog.show();
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
