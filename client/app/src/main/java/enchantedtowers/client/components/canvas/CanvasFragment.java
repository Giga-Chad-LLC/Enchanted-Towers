package enchantedtowers.client.components.canvas;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

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

    public static CanvasFragment newInstance() {
        Bundle args = new Bundle();

        CanvasFragment fragment = new CanvasFragment();
        fragment.setArguments(args);
        return fragment;
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_canvas, container, false);
        ConstraintLayout cl = rootView.findViewById(R.id.fragmentControlsLayout);

        addButtonToConstraintLayout(cl, R.id.changeColorButton, "Next color", false);
        addButtonToConstraintLayout(cl, R.id.clearCanvasButton, "Clear", true);

        return rootView;
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        canvasWidget = (CanvasWidget) (requireView().findViewById(R.id.canvasView));
        canvasWidget.setBrushColor(brushColors[currentCanvasBrushColor]);

        if (view.findViewById(R.id.changeColorButton) != null) {
            registerOnClickActionOnView(view, R.id.changeColorButton, this::nextColor);
        }
        if (view.findViewById(R.id.clearCanvasButton) != null) {
            registerOnClickActionOnView(view, R.id.clearCanvasButton, this::clearCanvas);
        }
    }

    @Override
    public void onDestroy() {
        canvasWidget.onDestroy();

        super.onDestroy();
    }

    private void registerOnClickActionOnView(View view, int itemId, Runnable action) {
        View clearCanvasButton = view.findViewById(itemId);
        clearCanvasButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                action.run();
            }
        });
    }
}