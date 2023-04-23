package enchantedtowers.client.components.canvas;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Arrays;

import enchantedtowers.client.R;
import enchantedtowers.client.interactors.canvas.CanvasDrawSpellInteractor;
import enchantedtowers.client.interactors.canvas.CanvasDrawStateInteractor;

public class CanvasAttackerFragment extends CanvasFragment {
    private int currentCanvasBrushColor = 0;
    private final int[] brushColors = {Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA};

    public static CanvasFragment newInstance() {
        return new CanvasAttackerFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflateFragment(R.layout.fragment_canvas, inflater, container);
        initAttackerLayout(rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View rootView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        initAttackerFunctionality(rootView);
    }

    public void nextColor() {
        currentCanvasBrushColor++;
        if (currentCanvasBrushColor >= brushColors.length) {
            currentCanvasBrushColor = 0;
        }

        if (canvasWidget != null) {
            canvasWidget.setBrushColor(brushColors[currentCanvasBrushColor]);
        }
    }

    public void clearCanvas() {
        if (canvasWidget != null) {
            canvasWidget.onClearCanvas();
        }
    }

    private void initAttackerFunctionality(View rootView) {
        canvasWidget = (rootView.findViewById(R.id.canvasView));
        canvasWidget.setInteractors(Arrays.asList(
                new CanvasDrawStateInteractor(),
                new CanvasDrawSpellInteractor(canvasWidget.getState(), canvasWidget)
        ));
        canvasWidget.setBrushColor(brushColors[currentCanvasBrushColor]);

        if (rootView.findViewById(R.id.changeColorButton) != null) {
            registerOnClickActionOnView(rootView, R.id.changeColorButton, this::nextColor);
        }
        if (rootView.findViewById(R.id.clearCanvasButton) != null) {
            registerOnClickActionOnView(rootView, R.id.clearCanvasButton, this::clearCanvas);
        }
    }

    private void initAttackerLayout(View rootView) {
        ConstraintLayout cl = rootView.findViewById(R.id.fragmentControlsLayout);

        addButtonToConstraintLayout(cl, R.id.changeColorButton, "Next color", false);
        addButtonToConstraintLayout(cl, R.id.clearCanvasButton, "Clear", true);
    }

    public void addButtonToConstraintLayout(
            ConstraintLayout constraintLayout,
            int buttonId,
            String buttonText,
            boolean constrainedToEnd
    ) {
        // create a new Button
        Button button = new Button(constraintLayout.getContext());
        button.setId(buttonId);
        button.setText(buttonText);
        // TODO: figure out how to create buttons with fixed size and certain text
        var layoutParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        if (constrainedToEnd) {
            layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        }
        else {
            layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        }

        button.setLayoutParams(layoutParams);

        // add the Button to the layout
        constraintLayout.addView(button);
    }
}
