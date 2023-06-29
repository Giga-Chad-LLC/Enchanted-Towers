package enchantedtowers.game_models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Enchantment {
    private final List<SpellTemplateDescription> spells;

    public Enchantment() {
        this.spells = new ArrayList<>();
    }

    public Enchantment(List<SpellTemplateDescription> spells) {
        this.spells = spells;
    }

    public List<SpellTemplateDescription> getTemplateDescriptions() {
        return Collections.unmodifiableList(spells);
    }
}
