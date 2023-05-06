package enchantedtowers.client.components.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import enchantedtowers.client.R;


public class TowerStatisticsDialogFragment extends BottomSheetDialogFragment {
    public static TowerStatisticsDialogFragment newInstance() {
        return new TowerStatisticsDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_tower_statistics_dialog, container, false);
        // get the views and attach the listener

        return view;

    }

}
