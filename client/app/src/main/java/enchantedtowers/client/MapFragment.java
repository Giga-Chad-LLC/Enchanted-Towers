package enchantedtowers.client;

import java.util.logging.Level;
import java.util.logging.Logger;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationListenerCompat;
import androidx.fragment.app.Fragment;

import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.LatLng;

import java.util.Objects;

import enchantedtowers.client.components.permissions.PermissionManager;


public class MapFragment extends Fragment {
    // TODO: make the following fields Optional<T>
    private GoogleMap googleMap;
    private AlertDialog GPSAlertDialog;
    private LocationListener locationUpdatesListener;
    private final Logger logger = Logger.getLogger(MapFragment.class.getName());


    public MapFragment() {
        // Required empty public constructor
    }

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.log(Level.INFO, "Created with context '" + requireContext().getClass().getName() + "'");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // initialize map fragment
        SupportMapFragment supportMapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map_fragment);

        // creating GPS alert dialog in advance
        createGPSEnableAlertDialog();

        // applying miscellaneous features on map view
        Objects.requireNonNull(supportMapFragment).getMapAsync(googleMap -> {
            // for convenient use if methods
            this.googleMap = googleMap;

            applyCustomGoogleMapStyle();

            // registering click listeners on MyLocation button and location point
            registerOnMyLocationButtonClickListener();
            registerOnMyLocationClickListener();

            // enabling user location
            if (PermissionManager.checkLocationPermission(requireContext())) {
                googleMap.setMyLocationEnabled(true);

                registerOnLocationUpdatesListener();

                // draw circle around last known location
                LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (lastKnownLocation != null) {
                     double latitude = lastKnownLocation.getLatitude();
                     double longitude = lastKnownLocation.getLongitude();
                    drawCircleAroundPoint(new LatLng(latitude, longitude));
                }
            }
            else {
                logger.log(Level.WARNING, "None of ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permissions granted. Cannot enable user location features on Google Maps");
            }
        });

        return view;
    }

    private void registerOnLocationUpdatesListener() throws SecurityException {
        Objects.requireNonNull(googleMap);

        LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);

        final int minTimeIntervalBetweenUpdatesMs = 30;
        final int minDistanceBetweenUpdateMeters = 1;

        this.locationUpdatesListener = new LocationListenerCompat() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                logger.log(Level.INFO, "New location: " + location);
                googleMap.clear();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                drawCircleAroundPoint(new LatLng(latitude, longitude));
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                logger.log(Level.WARNING, "Provider '" + provider + "' disabled");
                showGPSEnableDialogIfCreated();
            }
        };

        // registering event listener for location updates
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTimeIntervalBetweenUpdatesMs,
                minDistanceBetweenUpdateMeters,
                locationUpdatesListener);
    }


    private void createGPSEnableAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setMessage(
                "GPS is disabled. GPS is required to let application function properly.\nWould you mind enabling it?");

        builder.setPositiveButton("Enable GPS", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // Nothing
        });

        this.GPSAlertDialog = builder.create();
    }

    private void showGPSEnableDialogIfCreated() {
        if (GPSAlertDialog != null) {
            if (!GPSAlertDialog.isShowing()) {
                GPSAlertDialog.show();
            }
        }
        else {
            logger.log(Level.WARNING, "GPSAlertDialog is null (call 'createGPSEnableAlertDialog' to create)");
        }
    }

    private void registerOnMyLocationButtonClickListener() {
        Objects.requireNonNull(googleMap);

        googleMap.setOnMyLocationButtonClickListener(() -> {
            String message = "MyLocation button clicked";
            logger.log(Level.INFO, message);
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            return false;
        });
    }

    private void registerOnMyLocationClickListener() {
        Objects.requireNonNull(googleMap);
        googleMap.setOnMyLocationClickListener(location -> {
            String message = "Current location: " + location;
            logger.log(Level.INFO, message);
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        });
    }

    private void applyCustomGoogleMapStyle() {

        // applying map style
        boolean mapStyleAppliedSuccessfully = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));

        if (mapStyleAppliedSuccessfully) {
            logger.log(Level.INFO, "Map style applied successfully");
        }
        else {
            logger.log(Level.WARNING, "Map style applying failed");
        }
    }

    private void drawCircleAroundPoint(LatLng point) {
        Objects.requireNonNull(googleMap);

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(point);
        circleOptions.radius(200);
        circleOptions.strokeColor(Color.BLACK);
        circleOptions.fillColor(0x30ff0000);
        circleOptions.strokeWidth(2);

        googleMap.addCircle(circleOptions);
    }

    @Override
    public void onDestroy() {
        // this.GPSAlertDialog = null;

        // TODO: figure out whether it is even correct
        // unregistering location listener if not null
        if (locationUpdatesListener != null) {
            logger.log(Level.INFO, "Unregistering location listener");
            LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeUpdates(locationUpdatesListener);
        }

        super.onDestroy();
    }
}
