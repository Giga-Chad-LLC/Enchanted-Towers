package enchantedtowers.client.components.canvas;

import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CanvasState {
    private final CopyOnWriteArrayList<CanvasDrawable> items = new CopyOnWriteArrayList<>();
    private final Paint brush = new Paint();

    public CanvasState() {
        initBrush();
    }

//    /**
//     *
//     * @return
//     */
    // ??: a copy of stored items (copying is required for the thread safety)
    public List<CanvasDrawable> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(CanvasDrawable newItem) {
        items.add(newItem);
    }

    public void removeItem(int i) {
        items.remove(i);
    }

    public void clear() {
        items.clear();
    }

    public Paint getBrush() {
        return new Paint(brush);
    }

    public int getBrushColor() {
        return brush.getColor();
    }

    public void setBrushColor(int newColor) {
        brush.setColor(newColor);
    }

    public void setBrushColor(Color newColor) {
        brush.setColor(newColor.toArgb());
    }

    private void initBrush() {
        brush.setAntiAlias(true);
        brush.setColor(Color.BLACK);
        brush.setStyle(Paint.Style.STROKE);
        brush.setStrokeCap(Paint.Cap.ROUND);
        brush.setStrokeJoin(Paint.Join.ROUND);
        brush.setStrokeWidth(10f);
    }
}