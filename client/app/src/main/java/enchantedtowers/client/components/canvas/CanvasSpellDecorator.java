package enchantedtowers.client.components.canvas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.common.utils.proto.common.SpellType;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.utils.Vector2;

public class CanvasSpellDecorator implements CanvasDrawable {
    private final Path path;
    private final SpellType spellType;
    private final Spell spell;

    public CanvasSpellDecorator(SpellType spellType, Spell spell) {
        this.path = getPathBySpell(spell);
        this.spellType = spellType;
        this.spell = spell;
    }

    private Path getPathBySpell(Spell spell) {
        Path path = new Path();

        if (spell.getPointsCount() != 0) {
            Vector2 point = spell.getPointAt(0);
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
        brush.setColor(ClientUtils.getColorIdBySpellType(spellType));
        canvas.drawPath(path, brush);
        brush.setColor(previousColor);
    }
}