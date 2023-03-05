package enchantedtowers.client.components.canvas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import enchantedtowers.client.interactors.canvas.CanvasDrawEnchantmentInteractor;
import enchantedtowers.client.interactors.canvas.CanvasDrawStateInteractor;
import enchantedtowers.client.interactors.canvas.CanvasInteractor;

import java.util.ArrayList;


public class CanvasWidget extends View {
    private CanvasState state;
    private ArrayList<CanvasInteractor> interactors;

    public CanvasWidget(Context context) {
        super(context);
        init();
    }

    public CanvasWidget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CanvasWidget(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        state = new CanvasState();
        interactors = new ArrayList<>();

        interactors.add(
                new CanvasDrawStateInteractor()
        );
        interactors.add(
                new CanvasDrawEnchantmentInteractor(state)
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (CanvasInteractor interactor : interactors) {
            interactor.onDraw(state, canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = false;
        for (CanvasInteractor interactor : interactors) {
            result |= interactor.onTouchEvent(state, event.getX(), event.getY(), event.getAction());
        }

        if (result) {
            invalidate();
        }

        return result;
    }

    public void setBrushColor(int newColor) {
        state.setBrushColor(newColor);
    }

    public void setBrushColor(Color newColor) {
        state.setBrushColor(newColor.toArgb());
    }

    public void clearCanvas() {
        state.clear();
        invalidate();
    }
}