package enchantedtowers.client.components.canvas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import enchantedtowers.client.R;
import enchantedtowers.client.interactors.canvas.CanvasDrawStateInteractor;
import enchantedtowers.client.interactors.canvas.CanvasProtectionInteractor;

public class CanvasProtectorFragment extends DrawableCanvasFragment {

    public static CanvasFragment newInstance() {
        return new CanvasProtectorFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflateFragment(R.layout.fragment_canvas_setup_protection_wall, inflater, container);
    }

    @Override
    public void onViewCreated(@NonNull View rootView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        initProtectorFunctionality(rootView);
    }

    private void initProtectorFunctionality(View rootView) {
        canvasWidget = (rootView.findViewById(R.id.canvasView));
        initDrawingFunctionality(rootView, List.of(
                new CanvasDrawStateInteractor(),
                new CanvasProtectionInteractor(canvasWidget.getState(), canvasWidget)
        ));
    }
}
