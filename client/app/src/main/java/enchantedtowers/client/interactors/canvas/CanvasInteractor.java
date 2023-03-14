package enchantedtowers.client.interactors.canvas;

import android.graphics.Canvas;

import enchantedtowers.client.components.canvas.CanvasState;

public interface CanvasInteractor {
    void onDraw(CanvasState state, Canvas canvas);

    boolean onTouchEvent(CanvasState state, float x, float y, int motionEventType);
}