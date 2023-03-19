package enchantedtowers.client;

import java.util.logging.Level;
import java.util.logging.Logger;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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


public class MapFragment extends Fragment {
    public MapFragment() {
        // Required empty public constructor
    }

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private final Logger logger = Logger.getLogger(MapFragment.class.getName());

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.log(Level.INFO, "Created with context '" + requireContext().getClass().getName() + "'");
    }

    /*
    * TODO: read this: https://developer.android.com/training/permissions/requesting#java
    * TODO: read this: https://developer.android.com/training/location/permissions
    * TODO: read this: https://developer.android.com/reference/androidx/core/app/ActivityCompat
    * */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // initialize map fragment
        SupportMapFragment supportMapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map_fragment);

        Objects.requireNonNull(supportMapFragment).getMapAsync(googleMap -> {
            // When map is loaded

            // applying map style
            boolean mapStyleAppliedSuccessfully = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));

            if (mapStyleAppliedSuccessfully) {
                logger.log(Level.INFO, "Map style applied successfully");
            }
            else {
                logger.log(Level.WARNING, "Map style applying failed");
            }

            // setting 'OnMyLocation' click handlers
            googleMap.setOnMyLocationButtonClickListener(() -> {
                String message = "MyLocation button clicked";
                logger.log(Level.INFO, message);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                return false;
            });
            googleMap.setOnMyLocationClickListener(location -> {
                String message = "Current location: " + location;
                logger.log(Level.INFO, message);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            });

            // TODO: figure out how to solve this problem
            /*while (!googleMap.isMyLocationEnabled()) {
                enableMyLocation(googleMap);
            }*/
            enableMyLocation(googleMap);

            // TODO: what does it do?
            LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);

            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setMessage("GPS is disabled. Do you want to enable it?");

                builder.setPositiveButton("Enable GPS", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                });

                builder.setNegativeButton("Cancel", (dialog, which) -> {
                    //Nothing
                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();

                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireContext(), "Please turn on GPS", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0, 0, location -> {
                googleMap.clear();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                drawCircle(googleMap, new LatLng(latitude, longitude));
            });
        });

        return view;
    }

    private void enableMyLocation(GoogleMap map) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void drawCircle(GoogleMap map, LatLng point) {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(point);
        circleOptions.radius(200);
        circleOptions.strokeColor(Color.BLACK);
        circleOptions.fillColor(0x30ff0000);
        circleOptions.strokeWidth(2);
        map.addCircle(circleOptions);
    }
}
