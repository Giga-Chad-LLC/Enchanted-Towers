package enchantedtowers.client.interactors.map;

import android.graphics.Color;
import android.location.Location;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import enchantedtowers.common.utils.proto.responses.TowerResponse;
import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;
import io.grpc.StatusRuntimeException;

public class MapDrawTowersInteractor{
    private List<TowerResponse> towers;
    private final Logger logger = Logger.getLogger(MapDrawTowersInteractor.class.getName());

    public MapDrawTowersInteractor() {
        //empty
    }

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

    public List<TowerResponse> getTowers() {
        return towers;
    }

    public void execute(GoogleMap googleMap, TowersAggregationResponse response, Location userPosition) {
        try {
            towers = response.getTowersList();
            for (TowerResponse tower: towers){
                LatLng coordinatesForMarkerAtTower = new LatLng(tower.getPosition().getX(), tower.getPosition().getY());

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
