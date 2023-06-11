package enchantedtowers.client.components.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import enchantedtowers.client.R;
import enchantedtowers.client.components.adapters.DefendSpellbookElementAdapter;
import enchantedtowers.game_models.DefendSpell;
import enchantedtowers.game_models.SpellBook;

public class DefendSpellbookDialogFragment extends DialogFragment {
    private static class AdapterHolder {
        private final int adapterXmlId;
        private final List<DefendSpell> spells;
        private final DefendSpellbookElementAdapter adapter;

        AdapterHolder(List<DefendSpell> spells, int id) {
            this.spells = spells;
            this.adapterXmlId = id;
            this.adapter = new DefendSpellbookElementAdapter(spells);
        }

        int getDefendSpellTemplateId() {
            int result = -1;
            if (adapterXmlId == R.id.defend_spells_recycler_view_1) {
                result = 1;
            }
            else if (adapterXmlId == R.id.defend_spells_recycler_view_2) {
                result = 2;
            }
            else if (adapterXmlId == R.id.defend_spells_recycler_view_3) {
                result = 3;
            }

            return result;
        }
    }

    private final List<AdapterHolder> adapterHolders;

    public static DefendSpellbookDialogFragment newInstance() {
        return new DefendSpellbookDialogFragment();
    }

    private DefendSpellbookDialogFragment() {
        adapterHolders = new ArrayList<>();
        List<Integer> defendSpellsIds = List.of(R.id.defend_spells_recycler_view_1, R.id.defend_spells_recycler_view_2, R.id.defend_spells_recycler_view_3);
        for (int i = 0; i < defendSpellsIds.size(); ++i) {
            adapterHolders.add(new AdapterHolder(new ArrayList<>(), defendSpellsIds.get(i)));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_spellbook_dialog, container, false);

        // Customize and setup the dialog window
        // set title
        ((TextView)view.findViewById(R.id.spellbook_dialog_title)).setText("Defend spells");

        initSpellsLayout(inflater, (ViewGroup)view);

        Dialog dialog = getDialog();

        if (dialog != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // setting close button
        Button closeButton = view.findViewById(R.id.close_spellbook_button);
        closeButton.setOnClickListener(v -> dismiss());

        // clearing drawn defend spells
        adapterHolders.forEach(holder -> holder.spells.clear());

        // redraw
        for (var holder : adapterHolders) {
            setupRecyclerViewAdapter(view, holder.adapterXmlId, holder);
        }

        return view;
    }

    private void initSpellsLayout(LayoutInflater inflater, ViewGroup parentView) {
        createSpellBookCategory(inflater, parentView, R.id.defend_spells_recycler_view_1, "Defend spell 1");
        createSpellBookCategory(inflater, parentView, R.id.defend_spells_recycler_view_2, "Defend spell 2");
        createSpellBookCategory(inflater, parentView, R.id.defend_spells_recycler_view_3, "Defend spell 3");
    }

    public void createSpellBookCategory(LayoutInflater inflater, ViewGroup parentView, int recyclerViewId, String title) {
        // create category
        View categoryView = inflater.inflate(R.layout.element_spellbook_category, parentView, false);

        // set title
        TextView categoryTitle = categoryView.findViewById(R.id.spellbook_category_title);
        categoryTitle.setText(title);

        // set id of the recycler view
        View recycler = categoryView.findViewWithTag("spellbook_category_body");
        recycler.setId(recyclerViewId);

        // push created view inside parent
        ViewGroup targetContainer = parentView.findViewById(R.id.spellbook_scroll_view);
        targetContainer.addView(categoryView);
    }

    private void setupRecyclerViewAdapter(View view, Integer recyclerViewId, AdapterHolder holder) {
        RecyclerView recyclerView = view.findViewById(recyclerViewId);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
        recyclerView.setAdapter(holder.adapter);

        DefendSpell defendSpell = SpellBook.getDefendSpellTemplateById(holder.getDefendSpellTemplateId());

        holder.spells.add(defendSpell);
        holder.adapter.notifyItemRangeInserted(0, holder.adapter.getItemCount());
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
