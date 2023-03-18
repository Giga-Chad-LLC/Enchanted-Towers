package enchantedtowers.client.components.enchantment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import enchantedtowers.game_models.utils.Point;

public class SpellBook {
    static private boolean isInstantiated = false;
    static private final List<Spell> templates = new ArrayList<>();

    static public void instantiate(List<List<Point>> data) throws RuntimeException {
        if (isInstantiated) {
            throw new RuntimeException("EnchantmentBook singleton is already instantiated");
        }

        isInstantiated = true;
        for (var templatePoints : data) {
            templates.add(
                    new Spell(templatePoints)
            );
        }
    }

    static public List<Spell> getTemplates() {
        return Collections.unmodifiableList(templates);
    }

    private SpellBook() {
    }
}