package builders;

// game-models
import enchantedtowers.game_models.Enchantment;
import enchantedtowers.game_models.Spell;
// responses
import enchantedtowers.common.utils.proto.common.EnchantmentModel;


public class EnchantmentModelBuilder {
    public EnchantmentModel buildFrom(Enchantment enchantment) {
        EnchantmentModel.Builder enchantmentBuilder = EnchantmentModel.newBuilder();

        SpellModelBuilder spellBuilder = new SpellModelBuilder();
        for (Spell spell : enchantment.getSpells()) {
            enchantmentBuilder.addSpells(spellBuilder.buildFrom(spell));
        }

        return enchantmentBuilder.build();
    }
}
