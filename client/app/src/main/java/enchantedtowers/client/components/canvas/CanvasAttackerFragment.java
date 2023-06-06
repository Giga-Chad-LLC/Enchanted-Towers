package enchantedtowers.client.components.canvas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import enchantedtowers.client.R;
import enchantedtowers.client.interactors.canvas.CanvasAttackInteractor;
import enchantedtowers.client.interactors.canvas.CanvasDrawStateInteractor;

public class CanvasAttackerFragment extends DrawableCanvasFragment {
    public static CanvasFragment newInstance() {
        return new CanvasAttackerFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflateFragment(R.layout.fragment_canvas_attack, inflater, container);
    }

    @Override
    public void onViewCreated(@NonNull View rootView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        initAttackerFunctionality(rootView);
    }

    private void initAttackerFunctionality(View rootView) {
        canvasWidget = (rootView.findViewById(R.id.canvasView));
        initDrawingFunctionality(rootView, List.of(
                new CanvasDrawStateInteractor(),
                new CanvasAttackInteractor(canvasWidget.getState(), canvasWidget)
        ));
    }
}
