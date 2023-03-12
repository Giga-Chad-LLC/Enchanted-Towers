package enchantedtowers.client.components.canvas;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import enchantedtowers.client.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CanvasFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CanvasFragment extends Fragment {
    private int currentCanvasBrushColor = 0;
    private final int[] brushColors = { Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA };
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
        canvasWidget = (CanvasWidget)(requireView().findViewById(R.id.canvasView));
        canvasWidget.setBrushColor(brushColors[currentCanvasBrushColor]);

        registerOnClickActionOnView(view, "changeColorButton", this::nextColor);
        registerOnClickActionOnView(view, "clearCanvasButton", this::clearCanvas);
    }

    private void registerOnClickActionOnView(View view, String itemId, Runnable action) {
        @SuppressLint("DiscouragedApi")
        int canvasFragmentItemId = getResources().getIdentifier(itemId, "id", requireContext().getPackageName());
        View clearCanvasButton = view.findViewById(canvasFragmentItemId);
        clearCanvasButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                action.run();
            }
        });
    }
}