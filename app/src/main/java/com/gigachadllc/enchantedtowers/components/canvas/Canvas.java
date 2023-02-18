package com.gigachadllc.enchantedtowers.components.canvas;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.gigachadllc.enchantedtowers.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Canvas#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Canvas extends Fragment {
    private int currentCanvasBrushColor = 0;
    private int[] brushColors = { Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA };
    private CanvasView canvasView = null;

    public Canvas() {
        super(R.layout.fragment_canvas);
    }

    public void nextColor() {
        currentCanvasBrushColor++;
        if (currentCanvasBrushColor >= brushColors.length) {
            currentCanvasBrushColor = 0;
        }

        if (canvasView != null) {
            canvasView.setBrushColor(brushColors[currentCanvasBrushColor]);
        }
    }

    public void clearCanvas() {
        if (canvasView != null) {
            canvasView.clearCanvas();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        canvasView = (CanvasView)(getView().findViewById(R.id.canvasView));
        canvasView.setBrushColor(brushColors[currentCanvasBrushColor]);

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