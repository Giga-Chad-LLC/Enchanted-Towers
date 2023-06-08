package enchantedtowers.client.interactors.map;

import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import enchantedtowers.client.R;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.game_models.Tower;

public class DrawTowersOnMapInteractor {
    private final Logger logger = Logger.getLogger(DrawTowersOnMapInteractor.class.getName());
    private final List<Marker> installedTowerMarkers = new ArrayList<>();
    private Optional<Marker> playerMarker = Optional.empty();

    private final CircleOptions circleOptions = new CircleOptions()
            .radius(200)
            .strokeColor(Color.BLACK)
            .fillColor(Color.argb(48, 255, 0, 0))
            .strokeWidth(2);

    public void zoomToPlayerPosition(GoogleMap googleMap) {
        playerMarker.ifPresent(marker -> {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(marker.getPosition())
                    .zoom(8)
                    .build();

            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        });
    }

    public void updatePlayerIconPosition(Location playerLocation, GoogleMap googleMap) {
        var playerPosition = new LatLng(playerLocation.getLatitude(), playerLocation.getLongitude());

        // removing previous marker
        playerMarker.ifPresent(Marker::remove);

        MarkerOptions markerOptions = new MarkerOptions()
                .position(playerPosition)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.player_icon));

        Marker marker = googleMap.addMarker(markerOptions);

        if (marker != null) {
            playerMarker = Optional.of(marker);
            drawCircleAroundPoint(playerPosition, googleMap);
        }
        else {
            logger.warning("Cannot draw player icon for player position: " + playerPosition);
        }
    }


    private void drawCircleAroundPoint(LatLng point, GoogleMap googleMap) {
        Objects.requireNonNull(googleMap);
        circleOptions.center(point);
        googleMap.addCircle(circleOptions);
    }

    public void drawTowerIcons(GoogleMap googleMap, List<Tower> towers) {
        for (var tower : towers) {
            installNewMarker(googleMap, tower);
        }
    }

    public void updateMarkerAssociatedWithTower(GoogleMap googleMap, Tower tower) {
        Marker targetMarker = null;
        for (var marker : installedTowerMarkers) {
            if (marker.getTag() != null && ((Integer) marker.getTag()) == tower.getId()) {
                targetMarker = marker;
                break;
            }
        }

        if (targetMarker != null) {
            logger.info("Installing new marker with id " + tower.getId());
            targetMarker.remove();
            installedTowerMarkers.remove(targetMarker);
            installNewMarker(googleMap, tower);
        }
        else {
            logger.warning("No update performed: marker with id " + tower.getId() + " not found");
        }
    }

    private void installNewMarker(GoogleMap googleMap, Tower tower) {
        LatLng markerPosition = new LatLng(tower.getPosition().x, tower.getPosition().y);

        int towerIconId = determineTowerIconId(tower);

        var markerOptions = new MarkerOptions()
                .position(markerPosition)
                .icon(BitmapDescriptorFactory.fromResource(towerIconId));

        Marker marker = googleMap.addMarker(markerOptions);
        // setting tower id on marker
        if (marker != null) {
            marker.setTag(tower.getId());
            installedTowerMarkers.add(marker);
            drawCircleAroundPoint(marker.getPosition(), googleMap);
        }
    }

    private int determineTowerIconId(Tower tower) {
        int playerId = ClientStorage.getInstance().getPlayerId().get();
        boolean isPlayerOwner = tower.getOwnerId().isPresent() && tower.getOwnerId().get() == playerId;

        if (tower.isAbandoned()) {
            return R.drawable.abandoned_tower_icon;
        }
        else if (isPlayerOwner && tower.isUnderAttack()) {
            return R.drawable.owner_tower_under_attack_icon;
        }
        else if (isPlayerOwner) {
            return R.drawable.owner_tower_icon;
        }
        else if (tower.isUnderAttack()) {
            return R.drawable.enemy_tower_under_attack_icon;
        }
        else {
            return R.drawable.enemy_tower_icon;
        }
    }

}
