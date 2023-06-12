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
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.location.LocationListenerCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import enchantedtowers.client.MainActivity;
import enchantedtowers.client.MapActivity;
import enchantedtowers.client.R;
import enchantedtowers.client.components.dialogs.EnableGPSSuggestionDialog;
import enchantedtowers.client.components.permissions.PermissionManager;
import enchantedtowers.client.components.registry.TowersRegistry;
import enchantedtowers.client.components.registry.TowersRegistryManager;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.client.interactors.map.DrawTowersOnMapInteractor;
import enchantedtowers.common.utils.proto.requests.GameSessionTokenRequest;
import enchantedtowers.common.utils.proto.requests.JwtTokenRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.GameSessionTokenResponse;
import enchantedtowers.common.utils.proto.responses.ServerError;
import enchantedtowers.common.utils.proto.services.AuthServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import enchantedtowers.game_models.Tower;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;


public class MapFragment extends Fragment {
    private static class TowerUpdateSubscription {
        private final int towerId;
        private final TowersRegistryManager.Subscription callback;

        TowerUpdateSubscription(int towerId, TowersRegistryManager.Subscription callback) {
            this.towerId = towerId;
            this.callback = callback;
        }

        public int towerId() {
            return towerId;
        }

        public TowersRegistryManager.Subscription callback() {
            return callback;
        }
    }

    private Optional<GoogleMap> googleMap = Optional.empty();
    private Optional<EnableGPSSuggestionDialog> GPSDialog = Optional.empty();
    private Optional<LocationListener> locationUpdatesListener = Optional.empty();
    private final DrawTowersOnMapInteractor drawInteractor = new DrawTowersOnMapInteractor();
    private final List<TowerUpdateSubscription> onTowerUpdateSubscriptions = new ArrayList<>();
    private final Logger logger = Logger.getLogger(MapFragment.class.getName());
    private AuthServiceGrpc.AuthServiceStub authServiceStub;
    private Optional<ManagedChannel> channel = Optional.empty();
    private View fragmentMapView;


    public MapFragment() {}

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.log(Level.INFO, "Created with context '" + requireContext().getClass().getName() + "'");

        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();

        channel = Optional.of(Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create()).build());
        authServiceStub = AuthServiceGrpc.newStub(channel.get());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        this.fragmentMapView = view;

        // initialize map fragment
        SupportMapFragment supportMapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map_fragment);

        // creating GPS alert dialog in advance
        createGPSEnableAlertDialog();

        // applying miscellaneous features on map view
        Objects.requireNonNull(supportMapFragment).getMapAsync(googleMap_ -> {
            // for convenient use if methods
            this.googleMap = Optional.of(googleMap_);
            // setting map styles
            applyCustomGoogleMapStyle();

            // registering click listeners on "MyLocation" button, location point and markers
            registerOnMyLocationButtonClickListener();
            registerOnCustomMyLocationButtonClickListener();
            registerOnMarkerClickListener();

            // enabling user location and registering location updates listener
            enableUserLocationAndRegisterLocationUpdatesListener();

            if (ClientStorage.getInstance().getGameSessionToken().isPresent()) {
                establishGameSession(this::requestTowers);
            }
            else {
                // request game session token
                requestGameSessionToken(() -> establishGameSession(this::requestTowers));
            }
        });

        return view;
    }

    private void establishGameSession(Runnable onSuccessCallback) {
        String sessionToken = ClientStorage.getInstance().getGameSessionToken().get();
        GameSessionTokenRequest request = GameSessionTokenRequest.newBuilder()
                .setToken(sessionToken)
                .build();

        logger.info("Establishing game session connection...");
        authServiceStub.establishGameSession(request, new StreamObserver<>() {
            private Optional<ServerError> serverError = Optional.empty();
            @Override
            public void onNext(ActionResultResponse response) {
                if (response.hasError()) {
                    serverError = Optional.of(response.getError());
                }
                else {
                    logger.info("Game session established success=" + response.getSuccess());
                    // if establishment succeeded then run callback
                    if (response.getSuccess()) {
                        onSuccessCallback.run();
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("Error occurred: " + t.getMessage());
                t.printStackTrace();
                if (fragmentMapView != null) {
                    ClientUtils.showSnackbar(fragmentMapView, t.getMessage(), Snackbar.LENGTH_LONG);
                }

                // removing game session token
                ClientStorage.getInstance().resetGameSessionToken();
            }

            @Override
            public void onCompleted() {
                if (serverError.isPresent() && fragmentMapView != null) {
                    ClientUtils.showSnackbar(fragmentMapView, "Error occurred: " + serverError.get().getMessage(), Snackbar.LENGTH_LONG);
                }
                else {
                    logger.info("Game session ended");
                }

                // removing game session token
                ClientStorage.getInstance().resetGameSessionToken();
            }
        });
    }

    private void requestGameSessionToken(Runnable onSuccessCallback) {
        // form request (assume that jwt token already stored in storage)
        JwtTokenRequest request = JwtTokenRequest.newBuilder()
                .setToken(ClientStorage.getInstance().getJWTToken().get())
                .build();

        // requesting game session token
        logger.info("No game session token found. Requesting new one...");
        authServiceStub.createGameSessionToken(request, new StreamObserver<>() {
            private Optional<ServerError> serverError = Optional.empty();

            @Override
            public void onNext(GameSessionTokenResponse response) {
                if (response.hasError()) {
                    serverError = Optional.of(response.getError());
                }
                else {
                    // setting data into client storage (assuming that username and player id already set)
                    String gameSessionToken = response.getGameSessionToken();
                    ClientStorage.getInstance().setGameSessionToken(gameSessionToken);
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("Error occurred: " + t.getMessage());
                t.printStackTrace();
                if (fragmentMapView != null) {
                    ClientUtils.showSnackbar(fragmentMapView, t.getMessage(), Snackbar.LENGTH_LONG);
                }
            }

            @Override
            public void onCompleted() {
                if (serverError.isPresent()) {
                    ClientUtils.showSnackbar(fragmentMapView, "Error occurred: " + serverError.get().getMessage() + ". Try to relaunch the game", Snackbar.LENGTH_LONG);
                }
                else {
                    String sessionToken = ClientStorage.getInstance().getGameSessionToken().get();
                    logger.info("New game session token granted: " + sessionToken);
                    // running callback
                    onSuccessCallback.run();
                }
            }
        });
    }

    private void requestTowers() {
        // requesting towers from server
        logger.info("Requesting towers...");
        TowersRegistryManager.getInstance().requestTowers(new TowersRegistryManager.Callback() {
            @Override
            public void onError(Throwable t) {
                logger.severe("Error requesting towers: " + t.getMessage());
                ClientUtils.showSnackbar(fragmentMapView, "Unexpected error occurred while requesting towers data", Snackbar.LENGTH_LONG);
            }

            @Override
            public void onCompleted() {
                // drawing towers
                requireActivity().runOnUiThread(() -> {
                    logger.info("Drawing " + TowersRegistry.getInstance().getTowers().size() + " towers on map");
                    drawInteractor.drawTowerIcons(googleMap.get(), TowersRegistry.getInstance().getTowers());

                    // register for tower updates
                    registerTowersUpdatesSubscriptions();
                });
            }
        });
    }

    private void registerTowersUpdatesSubscriptions() {
        List<Tower> towers = TowersRegistry.getInstance().getTowers();

        for (var tower : towers) {
            int towerId = tower.getId();
            logger.info("Register subscription for updates of tower with id " + tower.getId());

            TowerUpdateSubscription subscription = new TowerUpdateSubscription(towerId,
                    updatedTower -> googleMap.ifPresent(map -> requireActivity().runOnUiThread(() -> drawInteractor.updateMarkerAssociatedWithTower(map, updatedTower))));

            onTowerUpdateSubscriptions.add(subscription);
            TowersRegistryManager.getInstance().subscribeOnTowerUpdates(towerId, subscription.callback());
        }
    }

    private void enableUserLocationAndRegisterLocationUpdatesListener() {
        if (PermissionManager.checkLocationPermission(requireContext())) {
            // disable location props to remove blue dot to replace it with custom player icon and custom MyLocationButton
            googleMap.get().getUiSettings().setMyLocationButtonEnabled(false);
            googleMap.get().setMyLocationEnabled(false);
            registerOnLocationUpdatesListener();
        }
        else {
            ClientUtils.showSnackbar(fragmentMapView, "Unexpected error occurred while requesting towers data", Snackbar.LENGTH_LONG);

            logger.warning("None of ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permissions granted. Cannot enable user location features on Google Maps");
        }
    }

    // registering listeners
    private void registerOnLocationUpdatesListener() throws SecurityException {
        Objects.requireNonNull(googleMap);
        LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);

        this.locationUpdatesListener = Optional.of(
                new LocationListenerCompat() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        logger.info("New player location: " + location);
                        drawInteractor.updatePlayerIconPosition(location, googleMap.get());
                    }

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                        logger.warning("Provider '" + provider + "' disabled");
                        showGPSEnableDialogIfAllowed();
                    }
                }
        );

        final int minTimeIntervalBetweenUpdatesMs = 30;
        final int minDistanceBetweenUpdateMeters = 1;

        // registering event listener for location updates
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTimeIntervalBetweenUpdatesMs,
                minDistanceBetweenUpdateMeters,
                locationUpdatesListener.get()
        );
    }

    private void registerOnMarkerClickListener() {
        this.googleMap.get().setOnMarkerClickListener(marker -> {
            // zoom in camera to marker position
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(marker.getPosition())
                    .zoom(8)
                    .build();

            Runnable showTowerStatsDialog = () -> {
                // show tower stats dialog
                Integer towerId = (Integer) marker.getTag();
                if (towerId != null) {
                    logger.info("Tower id stored in marker tag: '" + towerId + "'");
                    // TODO: dialogs must be dismissed
                    var dialog = TowerStatisticsDialogFragment.newInstance(towerId);
                    dialog.show(getParentFragmentManager(), dialog.getTag());
                }
                else {
                    logger.warning("Clicked marker does not store tower id");
                }
            };

            this.googleMap.get()
                    .animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), new GoogleMap.CancelableCallback() {
                @Override
                public void onCancel() {
                    showTowerStatsDialog.run();
                }

                @Override
                public void onFinish() {
                    showTowerStatsDialog.run();
                }
            });

            return true;
        });
    }

    private void registerOnMyLocationButtonClickListener() {
        googleMap.get().setOnMyLocationButtonClickListener(() -> false);
    }

    private void registerOnCustomMyLocationButtonClickListener() {
        Button myLocationButton = requireActivity().findViewById(R.id.custom_my_location_button);
        myLocationButton.setOnClickListener(view -> drawInteractor.zoomToPlayerPosition(googleMap.get()));
    }


    // helper methods

    private void createGPSEnableAlertDialog() {
        Runnable positiveCallback = () -> {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        };
        Runnable negativeCallback = () -> ClientUtils.showSnackbar(fragmentMapView, "GPS required for proper application functioning", Snackbar.LENGTH_LONG);

        GPSDialog = Optional.of(EnableGPSSuggestionDialog.newInstance(requireContext(), positiveCallback, negativeCallback));
    }

    private void showGPSEnableDialogIfAllowed() {
        if (GPSDialog.isPresent() && !GPSDialog.get().isShowing()) {
            GPSDialog.get().show();
        }
        else {
            logger.warning("GPS dialog either is null or is showing");
        }
    }

    private void applyCustomGoogleMapStyle() {
        // applying map style
        boolean mapStyleAppliedSuccessfully = googleMap.get().setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));

        if (mapStyleAppliedSuccessfully) {
            logger.info("Map style applied successfully");
        }
        else {
            ClientUtils.showSnackbar(fragmentMapView, "Unexpected error occurred: cannot apply custom map styles", Snackbar.LENGTH_LONG);
            logger.warning("Map style applying failed");
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

        // unregistering towers updates listeners
        for (var subscription : onTowerUpdateSubscriptions) {
            TowersRegistryManager.getInstance()
                    .unsubscribeFromTowerUpdates(subscription.towerId(), subscription.callback());
        }

        // shutting down towers manager
        TowersRegistryManager.getInstance().shutdown();

        // clearing map
        googleMap.ifPresent(GoogleMap::clear);

        // dismissing GPS dialog
        if (GPSDialog.isPresent() && GPSDialog.get().isShowing()) {
            GPSDialog.get().dismiss();
        }

        // shutting down grpc channel
        if (channel.isPresent()) {
            channel.get().shutdownNow();
            try {
                channel.get().awaitTermination(ServerApiStorage.getInstance().getChannelTerminationAwaitingTimeout(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException err) {
                err.printStackTrace();
            }
        }

        // removing game session token
        ClientStorage.getInstance().resetGameSessionToken();

        super.onDestroy();
    }
}
