package enchantedtowers.client.components.canvas;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.List;

import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.game_models.DefendSpell;
import enchantedtowers.game_models.utils.Vector2;

public class CanvasDefendSpellDecorator implements CanvasDrawable {
    private final DefendSpell spell;
    private final Vector2 offset;
    private final Path path;

    public CanvasDefendSpellDecorator(DefendSpell spell, Vector2 offset) {
        this.spell = spell;
        this.offset = offset;
        this.path = getPathByDefendSpell(spell, offset);
    }

    private Path getPathByDefendSpell(DefendSpell spell, Vector2 offset) {
        Path path = new Path();

        List<List<Vector2>> lines = spell.getPoints();

        for (var line : lines) {
            if (line.isEmpty()) {
                continue;
            }

            Vector2 p = line.get(0);
            path.moveTo((float)p.x, (float)p.y);
            for (int i = 1; i < line.size(); ++i) {
                p = line.get(i);
                path.lineTo((float)p.x, (float)p.y);
            }
        }

        Matrix mat = new Matrix();
        mat.setTranslate((float) offset.x, (float) offset.y);
        path.transform(mat);

        return path;
    }

    @Override
    public void draw(Canvas canvas, Paint brush) {
        int previousColor = brush.getColor();
        brush.setColor(ClientUtils.getDefendSpellColorId());
        canvas.drawPath(path, brush);
        brush.setColor(previousColor);
    }
}
