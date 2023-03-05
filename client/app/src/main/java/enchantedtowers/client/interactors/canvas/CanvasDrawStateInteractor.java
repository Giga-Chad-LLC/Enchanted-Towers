package enchantedtowers.client.interactors.canvas;

import android.graphics.Canvas;
import android.graphics.Paint;

import enchantedtowers.client.components.canvas.CanvasItem;
import enchantedtowers.client.components.canvas.CanvasState;

public class CanvasDrawStateInteractor implements CanvasInteractor {

    @Override
    public void onDraw(CanvasState state, Canvas canvas) {
        Paint brush = state.getBrush();

        for (CanvasItem item : state.items) {
            brush.setColor(item.getColor());
            item.draw(canvas, brush);
        }
    }

    @Override
    public boolean onTouchEvent(CanvasState state, float x, float y, int motionEventType) {
        return false;
    }
}