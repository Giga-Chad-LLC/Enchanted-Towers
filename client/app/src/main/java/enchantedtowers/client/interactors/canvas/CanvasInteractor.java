package enchantedtowers.client.interactors.canvas;

import android.graphics.Canvas;

import org.json.JSONException;

import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.common.utils.proto.requests.ToggleAttackerRequest;

public interface CanvasInteractor {
    void onDraw(CanvasState state, Canvas canvas);

    default void onExecutionInterrupt() {}

    /**
     * @param state           canvas state
     * @param x               touch location
     * @param y               touch location
     * @param motionEventType type of motion (UP, DOWN, MOVE)
     * @return `true` if event was handled, `false` otherwise.
     */
    boolean onTouchEvent(CanvasState state, float x, float y, int motionEventType);

    boolean onClearCanvas(CanvasState state);

    boolean onToggleSpectatingAttacker(ToggleAttackerRequest.RequestType requestType, CanvasState state);
}
