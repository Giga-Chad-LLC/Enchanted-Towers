package enchantedtowers.client.components.canvas;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import enchantedtowers.client.R;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.client.interactors.canvas.CanvasDrawStateInteractor;
import enchantedtowers.client.interactors.canvas.CanvasViewingInteractor;

public class CanvasViewingFragment extends CanvasFragment {
    public static CanvasFragment newInstance() {
        return new CanvasViewingFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflateFragment(R.layout.fragment_canvas_view_enchantment, inflater, container);
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
