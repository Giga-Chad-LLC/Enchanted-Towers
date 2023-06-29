package enchantedtowers.client.components.canvas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import enchantedtowers.client.R;
import enchantedtowers.client.components.adapters.ActiveDefendSpellsAdapter;
import enchantedtowers.client.interactors.canvas.CanvasAttackInteractor;
import enchantedtowers.client.interactors.canvas.CanvasDrawStateInteractor;

public class CanvasAttackerFragment extends DrawableCanvasFragment {
    private final ActiveDefendSpellsAdapter adapter;

    public static CanvasFragment newInstance() {
        return new CanvasAttackerFragment();
    }

    public CanvasAttackerFragment() {
        this.adapter = new ActiveDefendSpellsAdapter();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflateFragment(R.layout.fragment_canvas_attack, inflater, container);

        // set up defend spells adapter
        RecyclerView recyclerView = view.findViewById(R.id.active_defend_spells_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View rootView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        initAttackerFunctionality(rootView);
    }

    private void initAttackerFunctionality(View rootView) {
        canvasWidget = rootView.findViewById(R.id.canvasView);
        initDrawingFunctionality(rootView, List.of(
                new CanvasDrawStateInteractor(),
                new CanvasAttackInteractor(this, canvasWidget.getState(), canvasWidget)
        ));
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
}
