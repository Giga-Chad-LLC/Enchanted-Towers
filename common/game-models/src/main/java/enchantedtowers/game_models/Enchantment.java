package enchantedtowers.game_models;

public class Enchantment {
    public enum ElementType {
        EARTH,
        AIR,
        FIRE,
        WATER,
    }

    private final ElementType type;

    public Enchantment(ElementType type) {
        this.type = type;
    }

    public ElementType getType() {
        return type;
    }

}
