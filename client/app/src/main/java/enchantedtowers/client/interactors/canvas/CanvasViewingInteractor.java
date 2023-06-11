package enchantedtowers.client.interactors.canvas;

import enchantedtowers.client.components.canvas.CanvasSpellDecorator;
import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.client.components.registry.TowersRegistry;
import enchantedtowers.game_models.Enchantment;
import enchantedtowers.game_models.ProtectionWall;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_models.TemplateDescription;

public class CanvasViewingInteractor {
    private final ProtectionWall protectionWall;

    public CanvasViewingInteractor(int towerId, int protectionWallId) {
        protectionWall = TowersRegistry.getInstance()
                .getTowerById(towerId).get()
                .getProtectionWallById(protectionWallId).get();

        if (!protectionWall.isEnchanted()) {
            throw new RuntimeException("Protection wall with id " + protectionWallId +
                    " of tower with id " + towerId + " not enchanted. Cannot show the enchantment on the canvas!");
        }
    }

    public void addTemplateDescriptionsInCanvasState(CanvasState state) {
        Enchantment enchantment = protectionWall.getEnchantment().get();
        for (TemplateDescription description : enchantment.getTemplateDescriptions()) {
            Spell spell = SpellBook.getTemplateById(description.id());

            if (spell == null) {
                throw new RuntimeException("Spell template with id " + description.id() +
                        " not found. Cannot show the enchantment on the canvas!");
            }

            spell.setOffset(description.offset());
            CanvasSpellDecorator drawable = new CanvasSpellDecorator(spell);

            state.addItem(drawable);
        }
    }
}
