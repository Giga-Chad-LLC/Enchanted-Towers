package enchantedtowers.game_models;

import java.util.ArrayList;
import  java.util.List;

// game_models
import enchantedtowers.game_models.Spell;

public class Enchantment {
    private final List<Spell> spells;
    private final int casterLevel;


    public Enchantment(int casterLevel, List<Spell> spells) {
        this.casterLevel = casterLevel;
        this.spells = spells;
    }

    public List<Spell> getSpells() {
        return spells;
    }

    public int getCasterLevel() {
        return casterLevel;
    }
}
