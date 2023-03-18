package enchantedtowers.client.interactors.canvas;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;

import enchantedtowers.client.components.canvas.CanvasEnchantment;
import enchantedtowers.client.components.canvas.CanvasItem;
import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.client.components.enchantment.Enchantment;
import enchantedtowers.client.components.enchantment.EnchantmentBook;

public class CanvasDrawStateInteractor implements CanvasInteractor {

    @Override
    public void onDraw(CanvasState state, Canvas canvas) {
        Paint brush = state.getBrush();
        drawTemplates(state, canvas);

        for (CanvasItem item : state.items) {
            brush.setColor(item.getColor());
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

    private void drawTemplates(CanvasState state, Canvas canvas) {
        List<Enchantment> templates = EnchantmentBook.getTemplates();
        Paint brush = state.getBrush();

        for (Enchantment ench : templates) {
            canvas.drawPath(ench.getPath(), brush);
        }
    }
}