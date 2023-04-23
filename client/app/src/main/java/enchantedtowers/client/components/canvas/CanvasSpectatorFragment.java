package enchantedtowers.client.components.canvas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

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
        View rootView = inflateFragment(R.layout.fragment_canvas, inflater, container);
        initSpectatorLayout(rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View rootView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        initSpectatorFunctionality(rootView);
    }

    private void toggleSpectatingAttacker(ToggleAttackerRequest.RequestType requestType) {
        canvasWidget.onToggleSpectatingAttacker(requestType);
    }

    private void initSpectatorLayout(View rootView) {
        ConstraintLayout cl = rootView.findViewById(R.id.fragmentControlsLayout);

        addButtonToConstraintLayout(cl, R.id.showPreviousAttackerButton, "Previous", false);
        addButtonToConstraintLayout(cl, R.id.showNextAttackerButton, "Next", true);
    }

    private void initSpectatorFunctionality(View rootView) {
        canvasWidget = rootView.findViewById(R.id.canvasView);
        canvasWidget.setInteractors(Arrays.asList(
                new CanvasDrawStateInteractor(),
                new CanvasSpectateInteractor(canvasWidget.getState(), canvasWidget)
        ));

        if (rootView.findViewById(R.id.showPreviousAttackerButton) != null) {
            registerOnClickActionOnView(
                    rootView,
                    R.id.showPreviousAttackerButton,
                    () -> this.toggleSpectatingAttacker(ToggleAttackerRequest.RequestType.SHOW_PREV_ATTACKER)
            );
        }
        if (rootView.findViewById(R.id.showNextAttackerButton) != null) {
            registerOnClickActionOnView(
                    rootView,
                    R.id.showNextAttackerButton,
                    () -> this.toggleSpectatingAttacker(ToggleAttackerRequest.RequestType.SHOW_NEXT_ATTACKER)
            );
        }
    }
}
