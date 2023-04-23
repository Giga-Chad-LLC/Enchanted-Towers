package enchantedtowers.client.interactors.canvas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.List;

import enchantedtowers.client.components.canvas.CanvasDrawable;
import enchantedtowers.client.components.canvas.CanvasSpellDecorator;
import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellBook;

public class CanvasDrawStateInteractor implements CanvasInteractor {

    @Override
    public void onDraw(CanvasState state, Canvas canvas) {
        Paint brush = state.getBrush();

        // for DEBUG purposes
        drawTemplates(state, canvas);

        for (CanvasDrawable item : state.items) {
            item.draw(canvas, brush);

            // For DEBUG purposes (will be removed later)
//            if (item instanceof CanvasEnchantment) {
//                ((CanvasEnchantment)item).drawOnCanvasByPoints(canvas, brush);
//            }
        }
    }

    @Override
    public boolean onTouchEvent(CanvasState state, float x, float y, int motionEventType) {
        return false;
    }

    // For DEBUG purposes, will be removed later
    private void drawTemplates(CanvasState state, Canvas canvas) {
        List<Spell> templates = SpellBook.getTemplates();
        Paint brush = state.getBrush();

        for (Spell spell : templates) {
            new CanvasSpellDecorator(Color.BLACK, spell).draw(canvas, brush);
        }
    }
}