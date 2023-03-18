package enchantedtowers.client.components.canvas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import enchantedtowers.client.components.enchantment.Spell;
import enchantedtowers.game_models.utils.Point;

public class CanvasEnchantment implements CanvasItem {
    private final Path path;
    private final int color;
    private final Spell spell;

    public CanvasEnchantment(Path path, int color, Spell spell) {
        this.path = path;
        this.color = color;
        this.spell = spell;
    }


    public void drawOnCanvasByPoints(Canvas canvas, Paint brush) {
        if (spell.getPointsCount() == 0) {
            return;
        }

        Path newPath = new Path();
        Point point = spell.getPointAt(0);
        newPath.moveTo((float)point.x, (float)point.y);

        for (int i = 1; i < spell.getPointsCount(); ++i) {
            point = spell.getPointAt(i);
            newPath.lineTo((float)point.x, (float)point.y);
        }

        brush.setColor(Color.BLUE);
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