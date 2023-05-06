package enchantedtowers.client.interactors.map;

import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import enchantedtowers.game_models.Tower;
import io.grpc.StatusRuntimeException;

public class DrawTowersOnMapInteractor {
    private final Logger logger = Logger.getLogger(DrawTowersOnMapInteractor.class.getName());

    public void drawCircleAroundPoint(LatLng point, GoogleMap googleMap) {
        Objects.requireNonNull(googleMap);

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(point);
        circleOptions.radius(200);
        circleOptions.strokeColor(Color.BLACK);
        circleOptions.fillColor(0x30ff0000);
        circleOptions.strokeWidth(2);

        googleMap.addCircle(circleOptions);
    }

    public void execute(GoogleMap googleMap, List<Tower> towers, Location userPosition) {
        try {
            for (var tower : towers){
                LatLng coordinatesForMarkerAtTower = new LatLng(tower.getPosition().x, tower.getPosition().y);

                // TODO: refactor
                float[] results = new float[1];
                Location.distanceBetween(coordinatesForMarkerAtTower.latitude, coordinatesForMarkerAtTower.longitude,
                        userPosition.getLatitude(), userPosition.getLongitude(), results);

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(coordinatesForMarkerAtTower)
                        .icon(BitmapDescriptorFactory.defaultMarker(results[0] > 200 ? BitmapDescriptorFactory.HUE_AZURE: BitmapDescriptorFactory.HUE_RED));

                googleMap.addMarker(markerOptions);

                drawCircleAroundPoint(coordinatesForMarkerAtTower, googleMap);
            }
        }
        catch(StatusRuntimeException err) {
            logger.log(Level.WARNING, "MapDrawTowersInteractor has fallen", err.getStatus());
        }
    }

}
