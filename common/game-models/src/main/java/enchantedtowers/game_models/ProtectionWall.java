package enchantedtowers.game_models;

import java.util.Optional;

public class ProtectionWall {
    private static class WallState {
        private boolean broken;
        private boolean enchanted;

        public WallState(boolean broken, boolean enchanted) {
            this.broken = broken;
            this.enchanted = enchanted;
        }

        public boolean isBroken() {
            return broken;
        }

        public boolean isEnchanted() {
            return enchanted;
        }

        public void setBroken(boolean value) {
            this.broken = value;
        }

        public void setEnchanted(boolean value) {
            this.enchanted = value;
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

    public void setEnchantment(Enchantment enchantment) {
        this.enchantment = Optional.of(enchantment);
        this.state.setBroken(false);
        this.state.setEnchanted(true);
    }

    public void removeEnchantment() {
        this.enchantment = Optional.empty();
    }

    public Optional<Enchantment> getEnchantment() {
        return this.enchantment;
    }

    public boolean isBroken() {
        return state.isBroken();
    }

    public boolean isEnchanted() {
        return state.isEnchanted();
    }
}
