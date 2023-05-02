package enchantedtowers.game_models;

import java.util.ArrayList;
import java.util.List;

public class Enchantment {
    private final List<TemplateDescription> spells;

    public Enchantment() {
        this.spells = new ArrayList<>();
    }

    public Enchantment(List<TemplateDescription> spells) {
        this.spells = spells;
    }

    public List<TemplateDescription> getSpells() {
        return spells;
    }
}
