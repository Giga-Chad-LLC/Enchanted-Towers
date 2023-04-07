package enchantedtowers.game_models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import enchantedtowers.game_models.utils.Vector2;

public class SpellBook {
    static private boolean isInstantiated = false;
    static private final List<Spell> templates = new ArrayList<>();

    static public boolean isInstantiated() {
        return isInstantiated;
    }

    static public void instantiate(List<List<Vector2>> data) throws RuntimeException {
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