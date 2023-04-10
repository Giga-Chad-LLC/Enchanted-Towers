package enchantedtowers.client.components.canvas;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

import enchantedtowers.client.R;
import enchantedtowers.client.interactors.canvas.CanvasDrawStateInteractor;
import enchantedtowers.client.interactors.canvas.CanvasSpectateInteractor;

public class CanvasSpectatorFragment extends CanvasFragment {
    public static CanvasFragment newInstance() {
        return new CanvasSpectatorFragment();
    }

    @Override
    public void onViewCreated(@NonNull View rootView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        initSpectatorFunctionality(rootView);
    }

    private void initSpectatorFunctionality(View rootView) {
        canvasWidget = rootView.findViewById(R.id.canvasView);
        canvasWidget.setInteractors(Arrays.asList(
                new CanvasDrawStateInteractor(),
                new CanvasSpectateInteractor(canvasWidget.getState(), canvasWidget)
        ));
    }
}
