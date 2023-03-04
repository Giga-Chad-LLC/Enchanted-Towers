package com.gigachadllc.enchantedtowers.components.canvas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.gigachadllc.enchantedtowers.components.enchantments.Enchantment;

public class CanvasEnchantment implements CanvasItem {
    private final Path path;
    private final int color;
    private Enchantment enchantment;

    public CanvasEnchantment(Path path, int color, Enchantment enchantment) {
        this.path = path;
        this.color = color;
        this.enchantment = enchantment;
    }


    private void drawOnCanvasByPoints(Canvas canvas, Paint brush) {
        float[] points = enchantment.points;

        if (points.length == 0) {
            return;
        }

        Path newPath = new Path();
        newPath.moveTo(points[0], points[1]);

        for (int i = 2; i < points.length; i += 2) {
            newPath.lineTo(points[i], points[i + 1]);
        }

        brush.setColor(color);
        canvas.drawPath(newPath, brush);
    }


    @Override
    public void draw(Canvas canvas, Paint brush) {
        canvas.drawPath(path, brush);
    }

    @Override
    public int getColor() {
        return color;
    }
}
