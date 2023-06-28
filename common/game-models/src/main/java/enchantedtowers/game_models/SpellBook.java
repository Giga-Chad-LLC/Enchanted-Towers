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


    static public synchronized boolean isInstantiated() {
        return isInstantiated;
    }

    static public synchronized void instantiate(
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
                    new Spell(spell.getPoints(), spell.getSpellType())
            );
        }

        // defend spells
        for (var defendSpell : defendSpellsTemplatesData) {
            defendSpellsTemplates.put(
                    defendSpell.getId(),
                    new DefendSpell(defendSpell.getName(), defendSpell.getPoints())
            );
        }

        isInstantiated = true;
    }

    static public synchronized Map<Integer, DefendSpell> getDefendSpellsTemplates() {
        return Collections.unmodifiableMap(defendSpellsTemplates);
    }

    static public synchronized DefendSpell getDefendSpellTemplateById(int id) {
        if (defendSpellsTemplates.containsKey(id)) {
            return new DefendSpell(defendSpellsTemplates.get(id));
        }

        return null;
    }

    static public synchronized Map<Integer, Spell> getSpellTemplates() {
        return Collections.unmodifiableMap(spellTemplates);
    }

    static public synchronized Spell getSpellTemplateById(int id) {
        if (spellTemplates.containsKey(id)) {
            return new Spell(spellTemplates.get(id));
        }

        return null;
    }

    static public synchronized Map<Integer, Spell> getSpellTemplatesBySpellType(SpellType type) {
        Map <Integer, Spell> result = new HashMap<>();

        for (var template : spellTemplates.entrySet()) {
            int spellId = template.getKey();
            Spell spell = template.getValue();

            if (spell.getSpellType() == type) {
                result.put(
                        spellId,
                        spell
                );
            }
        }

        return Collections.unmodifiableMap(result);
    }

    static public List<SpellType> getAllSpellTypes() {
        // list is already unmodifiable, see List.of(...) description
        return allSpellTypes;
    }

    private SpellBook() {
    }
}
