package enchantedtowers.client.components.map;


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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import enchantedtowers.client.CanvasActivity;
import enchantedtowers.client.R;
import enchantedtowers.client.components.permissions.PermissionManager;
import enchantedtowers.client.components.registry.TowersRegistry;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.client.interactors.map.DrawTowersOnMapInteractor;
import enchantedtowers.game_models.Tower;
import io.grpc.StatusRuntimeException;



public class MapFragment extends Fragment {
    private Optional<GoogleMap> googleMap;
    private Optional<AlertDialog> GPSAlertDialog;
    private Optional<LocationListener> locationUpdatesListener;
    private final DrawTowersOnMapInteractor drawInteractor = new DrawTowersOnMapInteractor();
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

        // TODO: remove from here into input field
        ClientStorage.getInstance().setPlayerId(0);
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
        Objects.requireNonNull(supportMapFragment).getMapAsync(googleMap_ -> {
            // for convenient use if methods
            this.googleMap = Optional.of(googleMap_);

            // creating client stub
            logger.info("Creating blocking stub");

            // setting map styles
            applyCustomGoogleMapStyle();

            // registering click listeners on "MyLocation" button, location point and markers
            registerOnMyLocationButtonClickListener();
            registerOnMyLocationClickListener();
            registerOnMyMarkerClickListener();

            // enabling user location
            enableUserLocation();
        });

        return view;
    }

    private void enableUserLocation() {
        if (PermissionManager.checkLocationPermission(requireContext())) {
            googleMap.get().setMyLocationEnabled(true);

            registerOnLocationUpdatesListener();

            // draw circle around last known location
            LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (lastKnownLocation != null) {
                getTowers(lastKnownLocation);
                double latitude = lastKnownLocation.getLatitude();
                double longitude = lastKnownLocation.getLongitude();
                drawInteractor.drawCircleAroundPoint(new LatLng(latitude, longitude), googleMap.get());
            }
        }
        else {
            logger.log(Level.WARNING, "None of ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permissions granted. Cannot enable user location features on Google Maps");
        }
    }

    // registering listeners
    private void registerOnLocationUpdatesListener() throws SecurityException {
        Objects.requireNonNull(googleMap);

        LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);

        final int minTimeIntervalBetweenUpdatesMs = 30;
        final int minDistanceBetweenUpdateMeters = 1;

        this.locationUpdatesListener = Optional.of(
                new LocationListenerCompat() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        logger.log(Level.INFO, "New location: " + location);
                        googleMap.get().clear();
                        getTowers(location);
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        drawInteractor.drawCircleAroundPoint(new LatLng(latitude, longitude), googleMap.get());
                    }

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                        logger.log(Level.WARNING, "Provider '" + provider + "' disabled");
                        showGPSEnableDialogIfCreated();
                    }
                }
        );

        // registering event listener for location updates
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTimeIntervalBetweenUpdatesMs,
                minDistanceBetweenUpdateMeters,
                locationUpdatesListener.get());
    }

    private void registerOnMyMarkerClickListener() {
        this.googleMap.get().setOnMarkerClickListener(marker -> {

            Integer towerId = (Integer) marker.getTag();

            if (towerId != null) {
                logger.info("Tower id stored in marker tag: '" + towerId + "'");

                var dialog = TowerStatisticsDialogFragment.newInstance(towerId);
                dialog.show(getParentFragmentManager(), dialog.getTag());

                /*Intent intent = new Intent(getActivity(), CanvasActivity.class);
                startActivity(intent);*/
            }
            else {
                logger.warning("Clicked marker does not store tower id");
            }

            return false;
        });
    }

    private void registerOnMyLocationButtonClickListener() {
        googleMap.get().setOnMyLocationButtonClickListener(() -> {
            String message = "MyLocation button clicked";
            logger.log(Level.INFO, message);
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            return false;
        });
    }

    private void registerOnMyLocationClickListener() {
        Objects.requireNonNull(googleMap);
        googleMap.get().setOnMyLocationClickListener(location -> {
            String message = "Current location: " + location;
            logger.log(Level.INFO, message);
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        });
    }

    // helper methods

    private void createGPSEnableAlertDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(requireContext());
        alertDialogBuilder.setMessage("GPS is disabled. GPS is required to let application function properly.\n" +
                "Would you mind enabling it?");

        alertDialogBuilder.setPositiveButton("Enable GPS", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        });

        alertDialogBuilder.setNegativeButton("Cancel", (dialog, which) -> {
            // Nothing
        });

        this.GPSAlertDialog = Optional.of(alertDialogBuilder.create());
    }

    private void showGPSEnableDialogIfCreated() {
        if (GPSAlertDialog.isPresent() && !GPSAlertDialog.get().isShowing()) {
            GPSAlertDialog.get().show();
        }
        else {
            logger.log(Level.WARNING, "GPSAlertDialog either is null or is showing");
        }
    }

    private void applyCustomGoogleMapStyle() {
        // applying map style
        boolean mapStyleAppliedSuccessfully = googleMap.get().setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));

        if (mapStyleAppliedSuccessfully) {
            logger.log(Level.INFO, "Map style applied successfully");
        }
        else {
            logger.log(Level.WARNING, "Map style applying failed");
        }
    }

    private void getTowers(Location location) {
        try {
            logger.info("getTowers: requesting towers");
            // TODO: performance bottle neck since it is blocking the execution thread (android studio warns about application not responding)
            // TODO: change to async stub

            List<Tower> towers = TowersRegistry.getInstance().getTowers();
            logger.info("getTowers: towers=" + towers);

            drawInteractor.execute(googleMap.get(), towers, location);
        }
        catch(StatusRuntimeException err) {
            logger.log(Level.WARNING, "Towers request has fallen", err.getStatus());
        }
    }


    @Override
    public void onDestroy() {
        // unregistering location listener if not null
        if (locationUpdatesListener.isPresent()) {
            logger.log(Level.INFO, "Unregistering location listener");
            LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeUpdates(locationUpdatesListener.get());
        }
        super.onDestroy();
    }
}
