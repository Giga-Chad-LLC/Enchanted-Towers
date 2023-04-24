package enchantedtowers.client;


import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.location.LocationListenerCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import enchantedtowers.client.components.permissions.PermissionManager;
import enchantedtowers.client.interactors.map.MapDrawTowersInteractor;
import enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest;
import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;
import enchantedtowers.common.utils.proto.services.TowersServiceGrpc;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.StatusRuntimeException;



public class MapFragment extends Fragment {
    // TODO: make the following fields Optional<T>
    private GoogleMap googleMap;
    private AlertDialog GPSAlertDialog;
    private LocationListener locationUpdatesListener;
    private GoogleMap.OnMarkerClickListener markerClickListener;
    private final Logger logger = Logger.getLogger(MapFragment.class.getName());
    private TowersServiceGrpc.TowersServiceBlockingStub blockingStub;
    private final MapDrawTowersInteractor drawInteractor = new MapDrawTowersInteractor();

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

            this.googleMap.setOnMarkerClickListener(markerClickListener);

            // creating client stub
            logger.info("Creating blocking stub");
            blockingStub = TowersServiceGrpc.newBlockingStub(
                    Grpc.newChannelBuilder("10.0.2.2:50051", InsecureChannelCredentials.create()).build());

            applyCustomGoogleMapStyle();

            // registering click listeners on MyLocation button, location point and marker's
            registerOnMyLocationButtonClickListener();
            registerOnMyLocationClickListener();
            registerOnMyMarkerClickListener();
            this.googleMap.setOnMarkerClickListener(markerClickListener);

            // enabling user location
            if (PermissionManager.checkLocationPermission(requireContext())) {
                googleMap.setMyLocationEnabled(true);

                registerOnLocationUpdatesListener();

                // draw circle around last known location
                LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (lastKnownLocation != null) {
                    getTowers(lastKnownLocation);
                    double latitude = lastKnownLocation.getLatitude();
                     double longitude = lastKnownLocation.getLongitude();
                    drawInteractor.drawCircleAroundPoint(new LatLng(latitude, longitude), googleMap);
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
                getTowers(location);
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                drawInteractor.drawCircleAroundPoint(new LatLng(latitude, longitude), googleMap);
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

    private void registerOnMyMarkerClickListener() {
        Objects.requireNonNull(googleMap);
        markerClickListener = marker -> {
            Intent intent = new Intent(getActivity(), CanvasActivity.class);
            startActivity(intent);
            return false;
        };
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

    private void getTowers(Location location) {
        PlayerCoordinatesRequest request = PlayerCoordinatesRequest.newBuilder()
                .setX(location.getLatitude())
                .setY(location.getLongitude())
                .build();
        try {
            logger.info("getTowers: requesting towers");
            // TODO: performance bottle neck since it is blocking the execution thread (android studio warns about application not responding)
            // TODO: change to async stub
            TowersAggregationResponse response = blockingStub.getTowersCoordinates(request);
            logger.info("getTowers: towers=" + response.getTowersList());

            drawInteractor.execute(googleMap, response, location);
        }
        catch(StatusRuntimeException err) {
            logger.log(Level.WARNING, "Towers request has fallen", err.getStatus());
        }
    }


    @Override
    public void onDestroy() {
        // TODO: unregister all listeners
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
