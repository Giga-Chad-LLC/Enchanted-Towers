package enchantedtowers.client.components.canvas;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.Objects;

import enchantedtowers.client.R;
import enchantedtowers.client.components.adapters.ActiveDefendSpellsAdapter;
import enchantedtowers.client.components.dialogs.DefendSpellbookDialogFragment;
import enchantedtowers.client.interactors.canvas.CanvasDrawStateInteractor;
import enchantedtowers.client.interactors.canvas.CanvasSpectateInteractor;
import enchantedtowers.common.utils.proto.requests.ToggleAttackerRequest;

public class CanvasSpectatorFragment extends CanvasFragment {
    private final ActiveDefendSpellsAdapter adapter;
    private final DefendSpellbookDialogFragment defendSpellbookDialog;

    public static CanvasFragment newInstance(FragmentActivity parentActivity) {
        return new CanvasSpectatorFragment(parentActivity);
    }

    public CanvasSpectatorFragment(FragmentActivity parentActivity) {
        super();
        defendSpellbookDialog = DefendSpellbookDialogFragment.newInstance(
            parentActivity.getSupportFragmentManager()
        );
        adapter = new ActiveDefendSpellsAdapter();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflateFragment(R.layout.fragment_canvas_spectate, inflater, container);

        // set up defend spells adapter
        RecyclerView recyclerView = view.findViewById(R.id.active_defend_spells_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View rootView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        initSpectatorFunctionality(rootView);
    }

    public void addActiveDefendSpell(int defendSpellId, long totalDuration) {
        requireActivity().runOnUiThread(() -> {
            adapter.addItem(defendSpellId, totalDuration);
        });
    }

    public void removeActiveDefendSpell(int defendSpellId) {
        requireActivity().runOnUiThread(() -> {
            adapter.removeItem(defendSpellId);
        });
    }

    private void toggleSpectatingAttacker(ToggleAttackerRequest.RequestType requestType) {
        canvasWidget.onToggleSpectatingAttacker(requestType);
    }

    private void initSpectatorFunctionality(View rootView) {
        canvasWidget = rootView.findViewById(R.id.canvasView);
        canvasWidget.setInteractors(Arrays.asList(
                new CanvasDrawStateInteractor(),
                new CanvasSpectateInteractor(this, canvasWidget.getState(), canvasWidget)
        ));

        View showPreviousAttackerButton = rootView.findViewById(R.id.previous_attacker_button_icon);
        View showNextAttackerButton = rootView.findViewById(R.id.next_attacker_button_icon);

        showPreviousAttackerButton.setOnClickListener(v ->
                this.toggleSpectatingAttacker(ToggleAttackerRequest.RequestType.SHOW_PREV_ATTACKER));

        showNextAttackerButton.setOnClickListener(v ->
                this.toggleSpectatingAttacker(ToggleAttackerRequest.RequestType.SHOW_NEXT_ATTACKER));


        Button openDefendSpellbookButton = rootView.findViewById(R.id.apply_defend_spell_button);
        openDefendSpellbookButton.setOnClickListener(v -> defendSpellbookDialog.show(getParentFragmentManager(), defendSpellbookDialog.getTag()));
    }
}
