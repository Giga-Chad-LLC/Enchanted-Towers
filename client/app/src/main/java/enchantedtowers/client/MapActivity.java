package enchantedtowers.client;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import enchantedtowers.client.components.permissions.PermissionManager;


public class MapActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        ActivityResultLauncher<String[]> locationPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    System.out.println("INFO: " + result);

                    Boolean fineLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION, false);

                    Boolean coarseLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION,false);

                    assert fineLocationGranted != null;
                    assert coarseLocationGranted != null;

                    if (/*fineLocationGranted != null &&*/ fineLocationGranted) {
                        System.out.println("INFO: fineLocationGranted");
                        mountGoogleMapsFragment();
                    }
                    else if (/*coarseLocationGranted != null &&*/ coarseLocationGranted) {
                        System.out.println("INFO: coarseLocationGranted");
                    }
                    else {
                        System.out.println("INFO: No location access granted");
                    }
                }
        );

        if (PermissionManager.checkLocationPermission(this)) {
            // You can use the API that requires the permission.
            System.out.println("INFO: ACCESS_FINE_LOCATION & ACCESS_COARSE_LOCATION granted");
            mountGoogleMapsFragment();
        }
        else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected, and what
            // features are disabled if it's declined. In this UI, include a
            // "cancel" or "no thanks" button that lets the user continue
            // using your app without granting the permission.
            boolean rationalFineLocation = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
            boolean rationalCoarseLocation = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION);
            System.out.println("INFO: rationalFineLocation=" + rationalFineLocation + ", rationalCoarseLocation=" + rationalCoarseLocation);

            locationPermissionLauncher.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            locationPermissionLauncher.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
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
