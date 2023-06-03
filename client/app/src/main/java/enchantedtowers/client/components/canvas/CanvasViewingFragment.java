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

        CanvasViewingInteractor interactor = new CanvasViewingInteractor(towerId, wallId);
        interactor.addTemplateDescriptionsInCanvasState(canvasWidget.getState());

        canvasWidget.setInteractors(List.of(
                new CanvasDrawStateInteractor()
        ));
    }
}
