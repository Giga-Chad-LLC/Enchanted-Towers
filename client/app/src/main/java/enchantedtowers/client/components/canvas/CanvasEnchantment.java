package enchantedtowers.client.components.canvas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;

import enchantedtowers.client.components.enchantment.Enchantment;

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
        if (enchantment.getPointsCount() == 0) {
            return;
        }

        Path newPath = new Path();
        PointF point = enchantment.getPointAt(0);
        newPath.moveTo(point.x, point.y);

        for (int i = 1; i < enchantment.getPointsCount(); ++i) {
            point = enchantment.getPointAt(i);
            newPath.lineTo(point.x, point.y);
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