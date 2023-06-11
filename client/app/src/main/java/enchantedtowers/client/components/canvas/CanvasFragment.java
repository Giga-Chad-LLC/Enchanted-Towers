package enchantedtowers.client.components.canvas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import enchantedtowers.client.R;
import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.client.interactors.canvas.CanvasDrawStateInteractor;
import enchantedtowers.client.interactors.canvas.CanvasInteractor;
import enchantedtowers.client.interactors.canvas.CanvasProtectionInteractor;
import enchantedtowers.common.utils.proto.common.SpellType;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CanvasFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CanvasFragment extends Fragment {
    protected CanvasWidget canvasWidget = null;

    public CanvasFragment() {
        super(R.layout.fragment_canvas_attack);
    }

    public static CanvasFragment newInstance() {
        return new CanvasFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflateFragment(R.layout.fragment_canvas_attack, inflater, container);
    }

    @Override
    public void onViewCreated(@NonNull View rootView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        initCanvasFragmentFunctionality(rootView);
    }

    /**
     * Method should be called inside a {@code onCreateView} lifecycle method inside derived fragment classes.
     */
    protected View inflateFragment(int resId, LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(resId, container, false);
        //  Inflate shared layouts here
        return view;
    }

    private void initCanvasFragmentFunctionality(View rootView) {
        Button leaveButton = rootView.findViewById(R.id.leave_canvas_button);
        leaveButton.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    @Override
    public void onDestroy() {
        if (canvasWidget != null) {
            canvasWidget.onExecutionInterrupt();
        }
        super.onDestroy();
    }
}
