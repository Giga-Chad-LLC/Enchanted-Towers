package com.gigachadllc.enchantedtowers.components.drawable;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;

import com.gigachadllc.enchantedtowers.components.canvas.CanvasItem;

import java.util.ArrayList;

public class Enchantment extends CanvasItem {
    private final Path path;
    private final int color;
    private final ArrayList<PointF> points; // must be relative to the bounded box of path

    public Enchantment(Path path, int color, ArrayList<PointF> points, PointF offset) {
        this.path = path;
        this.color = color;
        this.points = points;
        this.setOffset(offset);
    }

    @Override
    public void drawOnCanvas(Canvas canvas, Paint brush) {
        drawOnCanvasByPath(canvas, brush);
    }

    public void drawOnCanvasByPath(Canvas canvas, Paint brush) {
        brush.setColor(color);
        canvas.drawPath(path, brush);
    }

    public void drawOnCanvasByPoints(Canvas canvas, Paint brush) {
        if (points.isEmpty()) {
            return;
        }

        Path newPath = new Path();
        newPath.moveTo(points.get(0).x, points.get(0).y);

        for (int i = 1; i < points.size(); i++) {
            newPath.lineTo(points.get(i).x, points.get(i).y);
        }

        brush.setColor(color);
        canvas.drawPath(newPath, brush);
    }
}
