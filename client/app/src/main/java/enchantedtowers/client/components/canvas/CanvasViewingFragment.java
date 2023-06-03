package enchantedtowers.client.components.canvas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Arrays;
import java.util.List;

import enchantedtowers.client.R;
import enchantedtowers.client.components.registry.TowersRegistry;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.client.interactors.canvas.CanvasAttackInteractor;
import enchantedtowers.client.interactors.canvas.CanvasDrawStateInteractor;
import enchantedtowers.client.interactors.canvas.CanvasViewingInteractor;
import enchantedtowers.game_models.Enchantment;
import enchantedtowers.game_models.ProtectionWall;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_models.TemplateDescription;

public class CanvasViewingFragment extends CanvasFragment {
    public static CanvasFragment newInstance() {
        return new CanvasViewingFragment();
    }

    @Override
    public void onViewCreated(@NonNull View rootView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        initViewerFunctionality(rootView);
    }
    private void initViewerFunctionality(View rootView) {
        canvasWidget = (rootView.findViewById(R.id.canvasView));

        // TowerId and WallId must be set in ClientStorage
        // We will create spells from enchantment and store them inside canvas state
        int towerId = ClientStorage.getInstance().getTowerId().get();
        int wallId = ClientStorage.getInstance().getProtectionWallId().get();

        // TODO: move into interactor
        CanvasViewingInteractor interactor = new CanvasViewingInteractor(towerId, wallId);
        interactor.addTemplateDescriptionsInCanvasState(canvasWidget.getState());

        /*
        ProtectionWall protectionWall = TowersRegistry.getInstance().getTowerById(towerId).get().getProtectionWallById(wallId).get();
        if (!protectionWall.isEnchanted()) {
            throw new RuntimeException("Protection wall with id " + wallId + " of tower with id " + towerId + " not enchanted. Cannot show the enchantment on the canvas!");
        }

        Enchantment enchantment = protectionWall.getEnchantment().get();
        for (TemplateDescription description : enchantment.getTemplateDescriptions()) {
            Spell spell = SpellBook.getTemplateById(description.id());
            if (spell == null) {
                throw new RuntimeException("Spell template with id " + description.id() + " not found. Cannot show the enchantment on the canvas!");
            }
            spell.setOffset(description.offset());
            CanvasSpellDecorator drawable = new CanvasSpellDecorator(description.spellType(), spell);
            canvasWidget.getState().addItem(drawable);
        }
        */

        canvasWidget.setInteractors(List.of(
                new CanvasDrawStateInteractor()
        ));
    }
}
