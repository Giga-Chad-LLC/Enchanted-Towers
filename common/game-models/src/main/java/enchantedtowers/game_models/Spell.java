package enchantedtowers.game_models;

// game_models.utils
import enchantedtowers.game_models.utils.Point;

public class Spell {
    private final ElementType element;
    private final SpellForm form;
    private final Point center;

    public Spell(ElementType element, SpellForm form, Point center) {
        this.element = element;
        this.form = form;
        this.center = center;
    }

    public ElementType getElement() {
        return element;
    }

    public SpellForm getForm() {
        return form;
    }

    public Point getCenter() {
        return center;
    }

    public enum ElementType {
        EARTH,
        AIR,
        FIRE,
        WATER,
    }

    public enum SpellForm {
        CIRCLE,
        ELLIPSE,
        L_SHAPE,
    }
}
