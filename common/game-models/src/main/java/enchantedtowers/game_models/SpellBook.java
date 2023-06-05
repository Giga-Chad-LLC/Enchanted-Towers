package enchantedtowers.game_models;

import enchantedtowers.common.utils.proto.common.SpellType;
import enchantedtowers.game_logic.json.DefendSpellsTemplatesProvider;
import enchantedtowers.game_logic.json.SpellsTemplatesProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellBook {
    static private boolean isInstantiated = false;
    static private final Map<Integer, Spell> spellTemplates = new HashMap<>();
    static private final Map<Integer, DefendSpell> defendSpellsTemplates = new HashMap<>();
    static private final List<SpellType> allSpellTypes = List.of(
        SpellType.FIRE_SPELL,
        SpellType.WATER_SPELL,
        SpellType.WIND_SPELL,
        SpellType.EARTH_SPELL
    );


    static public boolean isInstantiated() {
        return isInstantiated;
    }

    static public void instantiate(
            List<SpellsTemplatesProvider.SpellTemplateData> spellsTemplatesData,
            List<DefendSpellsTemplatesProvider.DefendSpellTemplateData> defendSpellsTemplatesData
    ) throws RuntimeException {
        if (isInstantiated) {
            throw new RuntimeException("EnchantmentBook singleton is already instantiated");
        }

        // spells
        for (var spell : spellsTemplatesData) {
            spellTemplates.put(
                    spell.getId(),
                    new Spell(spell.getPoints())
            );
        }

        // defend spells
        for (var defendSpell : defendSpellsTemplatesData) {
            defendSpellsTemplates.put(
                    defendSpell.getId(),
                    new DefendSpell(defendSpell.getPoints())
            );
        }

        isInstantiated = true;
    }

    static public Spell getSpellTemplateById(int id) {
        if (spellTemplates.containsKey(id)) {
            return new Spell(spellTemplates.get(id));
        }

        return null;
    }

    static public DefendSpell getDefendSpellTemplateById(int id) {
        if (defendSpellsTemplates.containsKey(id)) {
            return new DefendSpell(defendSpellsTemplates.get(id));
        }

        return null;
    }

    static public Map<Integer, Spell> getSpellTemplates() {
        return Collections.unmodifiableMap(spellTemplates);
    }

    static public List<SpellType> getAllSpellTypes() {
        // list is already unmodifiable, see List.of(...) description
        return allSpellTypes;
    }

    static public Map<Integer, DefendSpell> getDefendSpellsTemplates() {
        return Collections.unmodifiableMap(defendSpellsTemplates);
    }

    private SpellBook() {
    }
}