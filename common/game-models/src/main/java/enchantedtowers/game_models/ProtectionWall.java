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

    private int id;
    private final WallState state;
    Optional<Enchantment> enchantment;

    ProtectionWall(int id) {
        this.id = id;
        this.state = new WallState(false, false);
        this.enchantment = Optional.empty();
    }

    public int getId() {
        return id;
    }

    public boolean hasEnchantment() {
        return enchantment.isPresent();
    }

    public boolean isBroken() {
        return state.isBroken();
    }

    public boolean isEnchanted() {
        return state.isEnchanted();
    }

    public void setEnchantment(Enchantment enchantment) {
        this.enchantment = Optional.of(enchantment);
    }
}
