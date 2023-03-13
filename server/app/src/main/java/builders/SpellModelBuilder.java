package builders;

// game_models
import enchantedtowers.game_models.Spell;
// game_models.utils
import enchantedtowers.game_models.utils.Point;
// proto/common
import enchantedtowers.common.utils.proto.common.SpellModel;

public class SpellModelBuilder {
    public SpellModel buildFrom(Spell spell) {
        SpellModel.Builder builder = SpellModel.newBuilder();

        SpellModel.ElementType element = switch (spell.getElement()) {
            case EARTH -> SpellModel.ElementType.EARTH;
            case AIR -> SpellModel.ElementType.AIR;
            case FIRE -> SpellModel.ElementType.FIRE;
            case WATER -> SpellModel.ElementType.WATER;
        };

        SpellModel.SpellForm form = switch (spell.getForm()) {
            case CIRCLE -> SpellModel.SpellForm.CIRCLE;
            case ELLIPSE -> SpellModel.SpellForm.ELLIPSE;
            case L_SHAPE -> SpellModel.SpellForm.L_SHAPE;
        };

        builder.setElement(element);
        builder.setForm(form);

        builder.getCenterBuilder()
                .setX(spell.getCenter().getX())
                .setY(spell.getCenter().getY());

        return builder.build();
    }
}
