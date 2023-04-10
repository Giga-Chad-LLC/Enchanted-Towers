package enchantedtowers.client.interactors.canvas;

import android.graphics.Canvas;

import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.client.components.canvas.CanvasWidget;

public class CanvasSpectateInteractor implements CanvasInteractor {

    public CanvasSpectateInteractor(CanvasState state, CanvasWidget canvasWidget) {

    }

    @Override
    public void onDraw(CanvasState state, Canvas canvas) {

    }

    @Override
    public boolean onTouchEvent(CanvasState state, float x, float y, int motionEventType) {
        return false;
    }
}
