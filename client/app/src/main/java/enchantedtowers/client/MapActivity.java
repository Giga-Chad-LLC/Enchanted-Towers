package enchantedtowers.client;

import android.Manifest;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;

import java.util.Optional;

import enchantedtowers.client.components.dialogs.DefendSpellbookDialogFragment;
import enchantedtowers.client.components.dialogs.LocationRequestPermissionRationaleDialog;
import enchantedtowers.client.components.map.MapFragment;
import enchantedtowers.client.components.permissions.PermissionManager;
import enchantedtowers.client.components.providers.SpellBookProvider;
import enchantedtowers.client.components.utils.ClientUtils;


public class MapActivity extends BaseActivity {
    private final String[] locationPermissions = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private Optional<LocationRequestPermissionRationaleDialog> dialog = Optional.empty();



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
        SpellBookProvider.getInstance().provideSpellBook(this);
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
