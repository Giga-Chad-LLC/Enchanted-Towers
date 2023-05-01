package enchantedtowers.game_models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import enchantedtowers.game_models.utils.Vector2;

public class SpellBook {
    static private boolean isInstantiated = false;
    static private final Map<Integer, Spell> templates = new HashMap<>();

    static public boolean isInstantiated() {
        return isInstantiated;
    }

    static public void instantiate(List<SpellTemplate> data) throws RuntimeException {
        if (isInstantiated) {
            throw new RuntimeException("EnchantmentBook singleton is already instantiated");
        }

        isInstantiated = true;
        for (var template : data) {
            templates.put(
                    template.getId(),
                    new Spell(template.getPoints())
            );
        }
    }

    static public Spell getTemplateById(int id) {
        if (templates.containsKey(id)) {
            return new Spell(templates.get(id));
        }

        return null;
    }

    static public Map<Integer, Spell> getTemplates() {
        return Collections.unmodifiableMap(templates);
    }

    private SpellBook() {
    }
}