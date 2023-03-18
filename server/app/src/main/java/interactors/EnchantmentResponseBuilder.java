package interactors;

// game-models
import enchantedtowers.game_models.Enchantment;
// responses
import enchantedtowers.common.utils.proto.responses.EnchantmentResponse;

public class EnchantmentResponseBuilder {
    public EnchantmentResponse buildFrom(Enchantment enchantment) {
        EnchantmentResponse.ElementType type = switch (enchantment.getType()) {
            case EARTH -> EnchantmentResponse.ElementType.EARTH;
            case AIR -> EnchantmentResponse.ElementType.AIR;
            case FIRE -> EnchantmentResponse.ElementType.FIRE;
            case WATER -> EnchantmentResponse.ElementType.WATER;
        };

        EnchantmentResponse response = EnchantmentResponse.newBuilder().setElement(type).build();
        return response;
    }
}
