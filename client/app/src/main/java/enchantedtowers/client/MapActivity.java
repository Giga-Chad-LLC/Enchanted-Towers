package enchantedtowers.client;

import android.Manifest;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.util.Optional;

import enchantedtowers.client.components.dialogs.LocationRequestPermissionRationaleDialog;
import enchantedtowers.client.components.map.MapFragment;
import enchantedtowers.client.components.permissions.PermissionManager;
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
                    boolean fineLocationPermissionGranted = Boolean.TRUE.equals(result.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION, false));

                    boolean coarseLocationPermissionGranted = Boolean.TRUE.equals(result.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION, false));

                    if (fineLocationPermissionGranted && coarseLocationPermissionGranted) {
                        Toast.makeText(this,
                                "All required permissions granted. Thanks, enjoy the game!", Toast.LENGTH_LONG).show();
                        mountGoogleMapsFragment();
                    }
                    else if (coarseLocationPermissionGranted) {
                        Toast.makeText(this,
                                "Access of coarse location granted. Content might be limited.", Toast.LENGTH_LONG).show();
                        mountGoogleMapsFragment();
                    }
                    else {
                        Toast.makeText(this, "Content might be limited. Please, grant access of location.", Toast.LENGTH_LONG).show();
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
    }

    private void showLocationRequestPermissionRationale(ActivityResultLauncher<String[]> locationPermissionLauncher) {
        Runnable positiveCallback = () -> locationPermissionLauncher.launch(locationPermissions);
        Runnable negativeCallback = () -> Toast.makeText(this, "Content might be limited", Toast.LENGTH_LONG).show();
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
