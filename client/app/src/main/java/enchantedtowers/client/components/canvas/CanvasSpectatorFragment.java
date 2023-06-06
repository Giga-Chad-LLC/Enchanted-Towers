package enchantedtowers.client.components.canvas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

import enchantedtowers.client.R;
import enchantedtowers.client.interactors.canvas.CanvasDrawStateInteractor;
import enchantedtowers.client.interactors.canvas.CanvasSpectateInteractor;
import enchantedtowers.common.utils.proto.requests.ToggleAttackerRequest;

public class CanvasSpectatorFragment extends CanvasFragment {
    public static CanvasFragment newInstance() {
        return new CanvasSpectatorFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflateFragment(R.layout.fragment_canvas_spectate, inflater, container);
    }

    @Override
    public void onViewCreated(@NonNull View rootView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        initSpectatorFunctionality(rootView);
    }

    private void toggleSpectatingAttacker(ToggleAttackerRequest.RequestType requestType) {
        canvasWidget.onToggleSpectatingAttacker(requestType);
    }

    private void initSpectatorFunctionality(View rootView) {
        canvasWidget = rootView.findViewById(R.id.canvasView);
        canvasWidget.setInteractors(Arrays.asList(
                new CanvasDrawStateInteractor(),
                new CanvasSpectateInteractor(canvasWidget.getState(), canvasWidget)
        ));

        View showPreviousAttackerButton = rootView.findViewById(R.id.previous_attacker_button_icon);
        View showNextAttackerButton = rootView.findViewById(R.id.next_attacker_button_icon);

        showPreviousAttackerButton.setOnClickListener(v ->
                this.toggleSpectatingAttacker(ToggleAttackerRequest.RequestType.SHOW_PREV_ATTACKER));

        showNextAttackerButton.setOnClickListener(v ->
                this.toggleSpectatingAttacker(ToggleAttackerRequest.RequestType.SHOW_NEXT_ATTACKER));
    }
}
