package enchantedtowers.game_models;

import java.util.Optional;

public class ProtectionWall {
    private static class WallState {
        private final boolean broken;
        private final boolean enchanted;

        WallState(boolean broken, boolean enchanted) {
            this.broken = broken;
            this.enchanted = enchanted;
        }

        boolean isBroken() {
            return broken;
        }

        boolean isEnchanted() {
            return enchanted;
        }
    }

    private final WallState state;
    Optional<Enchantment> enchantment;

    ProtectionWall() {
        this.state = new WallState(false, false);
        this.enchantment = Optional.empty();
    }

    boolean hasEnchantment() {
        return enchantment.isPresent();
    }

    boolean isBroken() {
        return state.isBroken();
    }

    boolean isEnchanted() {
        return state.isEnchanted();
    }
}
