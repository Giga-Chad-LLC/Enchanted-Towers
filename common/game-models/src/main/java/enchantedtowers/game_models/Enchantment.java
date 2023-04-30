package enchantedtowers.game_models;

import java.util.ArrayList;
import java.util.List;

public class Enchantment {
    private final List<Spell> spells;


    Enchantment() {
        this.spells = new ArrayList<>();
    }

    Enchantment(List<Spell> spells) {
        this.spells = spells;
    }
}
