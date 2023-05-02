package enchantedtowers.client.components.canvas;

import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.common.utils.proto.common.SpellType;

public class CanvasState {
    private final CopyOnWriteArrayList<CanvasDrawable> items = new CopyOnWriteArrayList<>();
    private final Paint brush = new Paint();
    private SpellType selectedSpellType = SpellType.UNRECOGNIZED;

    public CanvasState() {
        initBrush();
    }

    public List<CanvasDrawable> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(CanvasDrawable newItem) {
        items.add(newItem);
    }

    public void removeItem(int i) {
        items.remove(i);
    }

    public void clear() {
        items.clear();
    }

    /**
     * @return a copy of brush that is used inside a {@code CanvasState} with all settings included
     */
    public Paint getBrushCopy() {
        return new Paint(brush);
    }

    public int getBrushColor() {
        return brush.getColor();
    }

    public SpellType getSelectedSpellType() {
        return selectedSpellType;
    }

    public void setSpellType(SpellType newSpellType) {
        selectedSpellType = newSpellType;
        brush.setColor(ClientUtils.getColorIdBySpellType(newSpellType));
    }

    private void initBrush() {
        brush.setAntiAlias(true);
        brush.setColor(ClientUtils.getColorIdBySpellType(selectedSpellType));
        brush.setStyle(Paint.Style.STROKE);
        brush.setStrokeCap(Paint.Cap.ROUND);
        brush.setStrokeJoin(Paint.Join.ROUND);
        brush.setStrokeWidth(10f);
    }
}