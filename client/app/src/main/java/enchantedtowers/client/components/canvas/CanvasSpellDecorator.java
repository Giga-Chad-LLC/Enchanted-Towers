package enchantedtowers.client.components.canvas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

import enchantedtowers.client.components.enchantment.Spell;
import enchantedtowers.game_models.utils.Point;

public class CanvasSpellDecorator implements CanvasDrawable {
    private final Path path;
    private final int color;
    private final Spell spell;

    public CanvasSpellDecorator(int color, Spell spell) {
        this.path = getPathBySpell(spell);
        this.color = color;
        this.spell = spell;
    }

    private Path getPathBySpell(Spell spell) {
        Path path = new Path();

        if (spell.getPointsCount() != 0) {
            Point point = spell.getPointAt(0);
            path.moveTo((float) point.x, (float) point.y);

            for (int i = 1; i < spell.getPointsCount(); i++) {
                point = spell.getPointAt(i);
                path.lineTo((float) point.x, (float) point.y);
            }
        }

        Matrix mat = new Matrix();
        mat.setTranslate((float) spell.getOffset().x, (float) spell.getOffset().y);
        path.transform(mat);

        return path;
    }

    public void drawOnCanvasByPoints(Canvas canvas, Paint brush) {
        Path newPath = getPathBySpell(spell);

        // to highlight that debug method is used
        brush.setColor(Color.BLACK);
        canvas.drawPath(newPath, brush);
    }


    @Override
    public void draw(Canvas canvas, Paint brush) {
        int previousColor = brush.getColor();
        brush.setColor(color);
        canvas.drawPath(path, brush);
        brush.setColor(previousColor);
    }

    @Override
    public int getColor() {
        return color;
    }
}