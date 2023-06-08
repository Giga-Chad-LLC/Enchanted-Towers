package enchantedtowers.client;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import enchantedtowers.client.components.dialogs.LocationRequestPermissionRationaleDialog;
import enchantedtowers.client.components.dialogs.SpellbookDialogFragment;
import enchantedtowers.client.components.fs.AndroidFileReader;
import enchantedtowers.client.components.map.MapFragment;
import enchantedtowers.client.components.permissions.PermissionManager;
import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.common.utils.proto.common.Empty;
import enchantedtowers.common.utils.proto.responses.SpellBookResponse;
import enchantedtowers.common.utils.proto.services.SpellBookServiceGrpc;
import enchantedtowers.common.utils.proto.services.TowerAttackServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import enchantedtowers.game_logic.EnchantmetTemplatesProvider;
import enchantedtowers.game_models.SpellBook;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;


public class MapActivity extends BaseActivity {
    private final String[] locationPermissions = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private Optional<LocationRequestPermissionRationaleDialog> dialog = Optional.empty();


    private SpellBookServiceGrpc.SpellBookServiceBlockingStub blockingStub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        ActivityResultLauncher<String[]> locationPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    View mapFrameLayout = findViewById(R.id.map_frame_layout);

                    boolean fineLocationPermissionGranted = Boolean.TRUE.equals(result.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION, false));

                    boolean coarseLocationPermissionGranted = Boolean.TRUE.equals(result.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION, false));

                    if (fineLocationPermissionGranted && coarseLocationPermissionGranted) {
                        ClientUtils.showSnackbar(mapFrameLayout, "All required permissions granted. Thanks, enjoy the game!", Snackbar.LENGTH_LONG);
                        mountGoogleMapsFragment();
                    }
                    else if (coarseLocationPermissionGranted) {
                        ClientUtils.showSnackbar(mapFrameLayout, "Access of coarse location granted. Content might be limited.", Snackbar.LENGTH_LONG);
                        mountGoogleMapsFragment();
                    }
                    else {
                        ClientUtils.showSnackbar(mapFrameLayout, "Content might be limited. Please, grant access of location.", Snackbar.LENGTH_LONG);
                    }
                }
        );

        new PermissionManager()
                .withPermissions(locationPermissions, this, this::mountGoogleMapsFragment)
                .otherwise(() -> {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        showLocationRequestPermissionRationale(locationPermissionLauncher);
                    }
                    else {
                        locationPermissionLauncher.launch(locationPermissions);
                    }
                });


        // initialize spell book
        // TODO: move this logic to the start up of the application (when logging in)
        if (!SpellBook.isInstantiated()) {
            try {
                String host = ServerApiStorage.getInstance().getClientHost();
                int port = ServerApiStorage.getInstance().getPort();
                ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                        .usePlaintext()
                        .build();
                blockingStub = SpellBookServiceGrpc.newBlockingStub(channel);
                SpellBookResponse response = blockingStub
                        .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                        .retrieveSpellBookAsJSON(Empty.newBuilder().build());

                if (response.hasError()) {
                    ClientUtils.showSnackbar(findViewById(android.R.id.content).getRootView(), response.getError().getMessage(), Snackbar.LENGTH_LONG);
                    throw new FileNotFoundException("retrieveSpellBookAsJSON::Received error: " + response.getError().getMessage());
                }

                List<EnchantmetTemplatesProvider.SpellTemplateData> data = EnchantmetTemplatesProvider.parseJson(
                    response.getJsonData()
                );

                SpellBook.instantiate(data);
            } catch (JSONException | FileNotFoundException e) {
                Log.e("JSON-CONFIG", e.getMessage());
                System.err.println(e.getMessage());
            }
        }
    }

    private void showLocationRequestPermissionRationale(ActivityResultLauncher<String[]> locationPermissionLauncher) {
        Runnable positiveCallback = () -> locationPermissionLauncher.launch(locationPermissions);
        Runnable negativeCallback = () -> ClientUtils.showSnackbar(
                findViewById(R.id.map_frame_layout), "Content might be limited", Snackbar.LENGTH_LONG);

        dialog = Optional.of(LocationRequestPermissionRationaleDialog.newInstance(this, positiveCallback, negativeCallback));
        dialog.get().show();
    }

    private void mountGoogleMapsFragment() {
        // create fragment
        Fragment mapFragment = MapFragment.newInstance();

        // mount fragment into layout
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.map_frame_layout, mapFragment)
                .commit();
    }

    @Override
    public void onDestroy() {
        if (dialog.isPresent() && dialog.get().isShowing()) {
            dialog.get().dismiss();
        }
        super.onDestroy();
    }
}
