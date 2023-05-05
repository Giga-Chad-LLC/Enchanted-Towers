package enchantedtowers.game_models;

import enchantedtowers.common.utils.proto.common.SpellType;
import enchantedtowers.game_logic.EnchantmetTemplatesProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellBook {
    static private boolean isInstantiated = false;
    static private final Map<Integer, Spell> templates = new HashMap<>();
    static private final List<SpellType> allSpellTypes = List.of(
        SpellType.FIRE_SPELL,
        SpellType.WATER_SPELL,
        SpellType.WIND_SPELL,
        SpellType.EARTH_SPELL
    );


    static public boolean isInstantiated() {
        return isInstantiated;
    }

    static public void instantiate(List<EnchantmetTemplatesProvider.SpellTemplateData> data) throws RuntimeException {
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

    static public List<SpellType> getAllSpellTypes() {
        // list is already unmodifiable, see List.of(...)
        return allSpellTypes;
    }

    private SpellBook() {
    }
}