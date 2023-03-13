package interactors;

// game-models
import enchantedtowers.game_models.Enchantment;
// responses
import enchantedtowers.common.utils.proto.common.EnchantmentModel;

public class EnchantmentModelBuilder {
    public EnchantmentModel buildFrom(Enchantment enchantment) {
        EnchantmentModel.ElementType type = switch (enchantment.getType()) {
            case EARTH -> EnchantmentModel.ElementType.EARTH;
            case AIR -> EnchantmentModel.ElementType.AIR;
            case FIRE -> EnchantmentModel.ElementType.FIRE;
            case WATER -> EnchantmentModel.ElementType.WATER;
        };

        EnchantmentModel model = EnchantmentModel.newBuilder().setElement(type).build();
        return model;
    }
}
