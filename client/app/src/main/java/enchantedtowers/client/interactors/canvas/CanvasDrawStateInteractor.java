package enchantedtowers.client.interactors.canvas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Map;

import enchantedtowers.client.components.canvas.CanvasDrawable;
import enchantedtowers.client.components.canvas.CanvasSpellDecorator;
import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.common.utils.proto.common.SpellType;
import enchantedtowers.common.utils.proto.requests.ToggleAttackerRequest;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellBook;

public class CanvasDrawStateInteractor implements CanvasInteractor {

    @Override
    public void onDraw(CanvasState state, Canvas canvas) {
        Paint brush = state.getBrushCopy();

        // for DEBUG purposes
        // drawTemplates(state, canvas);

        for (CanvasDrawable item : state.getItems()) {
            item.draw(canvas, brush);

            // For DEBUG purposes (will be removed later)
            /*if (item instanceof CanvasEnchantment) {
                ((CanvasEnchantment)item).drawOnCanvasByPoints(canvas, brush);
            }*/
        }
    }

    // For DEBUG purposes, will be removed later
    private void drawTemplates(CanvasState state, Canvas canvas) {
        Map<Integer, Spell> templates = SpellBook.getSpellTemplates();
        Paint brush = state.getBrushCopy();

        for (Spell spell : templates.values()) {
            new CanvasSpellDecorator(SpellType.UNRECOGNIZED, spell).draw(canvas, brush);
        }
    }
}
