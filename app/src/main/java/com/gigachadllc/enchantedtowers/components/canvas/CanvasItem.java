package com.gigachadllc.enchantedtowers.components.canvas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

public abstract class CanvasItem {
    protected PointF canvasOffset = new PointF(0, 0);
    abstract public void drawOnCanvas(Canvas canvas, Paint brush);

    public void setOffset(PointF offset) {
        canvasOffset.x = offset.x;
        canvasOffset.y = offset.y;
    }
}
