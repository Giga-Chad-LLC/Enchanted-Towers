package enchantedtowers.client.components.enchantment;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import enchantedtowers.game_models.utils.Point;

public class EnchantmentBook {
    static private boolean isInstantiated = false;
    static private final List<Enchantment> templates = new ArrayList<>();

    static public void instantiate(List<List<Point>> data) throws RuntimeException {
        if (isInstantiated) {
            throw new RuntimeException("EnchantmentBook singleton is already instantiated");
        }

        isInstantiated = true;
        for (var templatePoints : data) {
            templates.add(
                    new Enchantment(templatePoints)
            );
        }
    }

    static public List<Enchantment> getTemplates() {
        return Collections.unmodifiableList(templates);
    }

    private EnchantmentBook() {}
}