package enchantedtowers.game_models;

public class ProtectionWall {
    private final WallState state;
    private final Enchantment enchantment;

    private record WallState(boolean isEnchanted, boolean isBroken) {}

    public ProtectionWall(Enchantment enchantment, boolean isBroken) {
        boolean isEnchanted = enchantment != null;
        this.state = new WallState(isEnchanted, isBroken);
        this.enchantment = enchantment;
    }

    public ProtectionWall(Enchantment enchantment) {
        this(enchantment, false);
    }

    public ProtectionWall() {
        this(null, false);
    }

    public boolean isEnchanted() {
        return state.isEnchanted();
    }

    public boolean isBroken() {
        return state.isBroken();
    }

    public Enchantment getEnchantment() {
        return enchantment;
    }
}
