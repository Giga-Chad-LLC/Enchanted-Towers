package enchantedtowers.client.components.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import enchantedtowers.client.R;
import enchantedtowers.client.components.adapters.SpellbookElementAdapter;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellBook;

public class SpellbookDialogFragment extends DialogFragment {
    private final List<Spell> fireSpells;
    private final SpellbookElementAdapter fireAdapter;

    public static SpellbookDialogFragment newInstance() {
        return new SpellbookDialogFragment();
    }

    private SpellbookDialogFragment() {
        fireSpells = new ArrayList<>();
        fireAdapter = new SpellbookElementAdapter(fireSpells);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_spellbook_dialog, container, false);
        // Customize and setup the dialog window
        Dialog dialog = getDialog();

        if (dialog != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        RecyclerView recyclerView = view.findViewById(R.id.fire_spells_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(fireAdapter);

        fireSpells.addAll(SpellBook.getTemplates().values());
        System.out.println("fireSpells size: " + fireSpells.size());
        fireAdapter.notifyItemRangeInserted(0, fireAdapter.getItemCount());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Adjust the dialog size and position if needed
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }
}
