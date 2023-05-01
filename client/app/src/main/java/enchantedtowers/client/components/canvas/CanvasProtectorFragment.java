package enchantedtowers.client.components.canvas;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Arrays;

import enchantedtowers.client.R;
import enchantedtowers.client.interactors.canvas.CanvasAttackInteractor;
import enchantedtowers.client.interactors.canvas.CanvasDrawStateInteractor;
import enchantedtowers.client.interactors.canvas.CanvasProtectionInteractor;

public class CanvasProtectorFragment extends CanvasFragment {
    private int currentCanvasBrushColor = 0;
    private final int[] brushColors = {Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA};

    public static CanvasFragment newInstance() {
        return new CanvasProtectorFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflateFragment(R.layout.fragment_canvas, inflater, container);
        initProtectorLayout(rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View rootView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        initProtectorFunctionality(rootView);
    }

    private void nextColor() {
        currentCanvasBrushColor++;
        if (currentCanvasBrushColor >= brushColors.length) {
            currentCanvasBrushColor = 0;
        }

        if (canvasWidget != null) {
            canvasWidget.setBrushColor(brushColors[currentCanvasBrushColor]);
        }
    }

    private void clearCanvas() {
        if (canvasWidget != null) {
            canvasWidget.onClearCanvas();
        }
    }

    private void submitCanvas() {
        if (canvasWidget != null) {
            canvasWidget.onSubmitCanvas();
        }
    }

    private void initProtectorFunctionality(View rootView) {
        canvasWidget = (rootView.findViewById(R.id.canvasView));
        canvasWidget.setInteractors(Arrays.asList(
                new CanvasDrawStateInteractor(),
                new CanvasProtectionInteractor(canvasWidget.getState(), canvasWidget)
        ));
        canvasWidget.setBrushColor(brushColors[currentCanvasBrushColor]);

        if (rootView.findViewById(R.id.changeColorButton) != null) {
            registerOnClickActionOnView(rootView, R.id.changeColorButton, this::nextColor);
        }
        if (rootView.findViewById(R.id.clearCanvasButton) != null) {
            registerOnClickActionOnView(rootView, R.id.clearCanvasButton, this::clearCanvas);
        }
        if (rootView.findViewById(R.id.submitCanvasButton) != null) {
            registerOnClickActionOnView(rootView, R.id.submitCanvasButton, this::submitCanvas);
        }
    }

    private void initProtectorLayout(View rootView) {
        ConstraintLayout cl = rootView.findViewById(R.id.fragmentControlsLayout);

        addButtonToConstraintLayout(cl, R.id.changeColorButton, "Next color", false, 20, 0);
        addButtonToConstraintLayout(cl, R.id.clearCanvasButton, "Clear", true, 20, 0);
        addButtonToConstraintLayout(cl, R.id.submitCanvasButton, "Submit", false, 470, 0);
    }
}
