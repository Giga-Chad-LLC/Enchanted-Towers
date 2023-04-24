package enchantedtowers.client;

import android.Manifest;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import enchantedtowers.client.components.permissions.PermissionManager;


public class MapActivity extends AppCompatActivity {
    private final String[] locationPermissions = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        ActivityResultLauncher<String[]> locationPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean fineLocationPermissionGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION, false);

                    Boolean coarseLocationPermissionGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION,false);

                    // TODO: change Boolean to boolean, possible?
                    assert fineLocationPermissionGranted != null;
                    assert coarseLocationPermissionGranted != null;

                    if (fineLocationPermissionGranted && coarseLocationPermissionGranted) {
                        Toast.makeText(this,
                                "All required permissions granted. Thanks, enjoy the game!", Toast.LENGTH_LONG).show();
                        mountGoogleMapsFragment();
                    }
                    else if (coarseLocationPermissionGranted) {
                        Toast.makeText(this,
                                "Access of coarse location granted. Content might be limited", Toast.LENGTH_LONG).show();
                        mountGoogleMapsFragment();
                    }
                    else {
                        Toast.makeText(this, "Content might be limited", Toast.LENGTH_LONG).show();
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
        // show rationale
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        final String rationale = """
                                Access to device location is required for the application to function properly,
                                e.g. to show your precise location on a map and determine distances between towers.
                                
                                Please, grant the device location permission to the application to gain full-fledged user experience.
                                """;
        builder.setMessage(rationale);

        builder.setPositiveButton("Continue", (dialog, which) -> {
            locationPermissionLauncher.launch(locationPermissions);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Toast.makeText(this, "Content might be limited", Toast.LENGTH_LONG).show();
        });

        builder.create().show();
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
}
