package enchantedtowers.client;

import java.util.logging.Level;
import java.util.logging.Logger;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.MapStyleOptions;

import java.util.Objects;


public class MapFragment extends Fragment {
    public MapFragment() {
        // Required empty public constructor
    }

    private final Logger logger = Logger.getLogger(MapFragment.class.getName());


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MapFragment.
     */
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

        Objects.requireNonNull(supportMapFragment).getMapAsync(googleMap -> {
            // When map is loaded
            boolean mapStyleAppliedSuccessfully = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));

            if (mapStyleAppliedSuccessfully) {
                logger.log(Level.INFO, "Map style applied successfully");
            }
            else {
                logger.log(Level.WARNING, "Map style applying failed");
            }

            googleMap.setOnMapClickListener(latLng -> {
                // remove all markers
                // googleMap.clear();
                // animating to zoom the marker
                // googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,10));
            });
        });

        return view;
    }
}
