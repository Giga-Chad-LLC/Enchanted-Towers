package enchantedtowers.client.interactors.map;

import android.graphics.Color;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import enchantedtowers.client.R;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.game_models.Tower;

public class DrawTowersOnMapInteractor {
    private final Logger logger = Logger.getLogger(DrawTowersOnMapInteractor.class.getName());
    private final List<Marker> installedMarkers = new ArrayList<>();

    private final CircleOptions circleOptions = new CircleOptions()
            .radius(200)
            .strokeColor(Color.BLACK)
            .fillColor(Color.argb(48, 255, 0, 0))
            .strokeWidth(2);

    public void drawCircleAroundPoint(LatLng point, GoogleMap googleMap) {
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
        for (var marker : installedMarkers) {
            if (marker.getTag() != null && ((Integer) marker.getTag()) == tower.getId()) {
                targetMarker = marker;
                break;
            }
        }

        if (targetMarker != null) {
            logger.info("Installing new marker with id " + tower.getId());
            targetMarker.remove();
            installedMarkers.remove(targetMarker);
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
            installedMarkers.add(marker);
            drawCircleAroundPoint(marker.getPosition(), googleMap);
        }
    }

    private int determineTowerIconId(Tower tower) {
        int playerId = ClientStorage.getInstance().getPlayerId().get();

        if (tower.isAbandoned()) {
            return R.drawable.abandoned_tower_icon;
        }
        if (tower.getOwnerId().isPresent() && tower.getOwnerId().get() == playerId) {
            return R.drawable.owner_tower_icon;
        }
        return R.drawable.enemy_tower_icon;
    }

}
