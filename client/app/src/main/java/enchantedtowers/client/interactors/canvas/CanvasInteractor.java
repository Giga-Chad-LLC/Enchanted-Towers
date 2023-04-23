package enchantedtowers.client.interactors.canvas;

import android.graphics.Canvas;

import org.json.JSONException;

import enchantedtowers.client.components.canvas.CanvasState;

public interface CanvasInteractor {
    void onDraw(CanvasState state, Canvas canvas);

    /**
     * @param state           canvas state
     * @param x               touch location
     * @param y               touch location
     * @param motionEventType type of motion (UP, DOWN, MOVE)
     * @return `true` if event was handled, `false` otherwise.
     */
    boolean onTouchEvent(CanvasState state, float x, float y, int motionEventType);
}