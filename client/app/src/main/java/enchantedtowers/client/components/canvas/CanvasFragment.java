package enchantedtowers.client.components.canvas;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.View;

import enchantedtowers.client.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CanvasFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CanvasFragment extends Fragment {
    private int currentCanvasBrushColor = 0;
    private final int[] brushColors = {Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA};
    private CanvasWidget canvasWidget = null;

    public CanvasFragment() {
        super(R.layout.fragment_canvas);
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
            canvasWidget.clearCanvas();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        canvasWidget = (CanvasWidget) (getView().findViewById(R.id.canvasView));
        canvasWidget.setBrushColor(brushColors[currentCanvasBrushColor]);

        int changeColorButtonId = getResources().getIdentifier("changeColorButton", "id", getContext().getPackageName());
        View changeColorButton = view.findViewById(changeColorButtonId);
        changeColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextColor();
            }
        });


        int clearCanvasButtonId = getResources().getIdentifier("clearCanvasButton", "id", getContext().getPackageName());
        View clearCanvasButton = view.findViewById(clearCanvasButtonId);
        clearCanvasButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearCanvas();
            }
        });
    }
}