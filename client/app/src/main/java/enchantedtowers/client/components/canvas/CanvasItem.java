package enchantedtowers.client.components.canvas;

import android.graphics.Canvas;
import android.graphics.Paint;

public interface CanvasItem {
    void draw(Canvas canvas, Paint brush);
    int getColor();
}