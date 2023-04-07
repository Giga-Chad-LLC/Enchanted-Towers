package enchantedtowers.client.components.canvas;

import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;

public class CanvasState {
    public ArrayList<CanvasDrawable> items;
    private final Paint brush = new Paint();

    public CanvasState() {
        items = new ArrayList<>();
        initBrush();
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