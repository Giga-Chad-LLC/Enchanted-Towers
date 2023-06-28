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
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import enchantedtowers.client.R;
import enchantedtowers.client.components.adapters.DefendSpellbookElementAdapter;
import enchantedtowers.game_models.DefendSpell;
import enchantedtowers.game_models.SpellBook;

public class DefendSpellbookDialogFragment extends DialogFragment {
    private static class AdapterHolder {
        private final int adapterXmlId;
        private final DefendSpellbookElementAdapter adapter;

        AdapterHolder(int xmlId, int spellId, FragmentManager parentFragmentManager, DialogFragment parentDialog) {
            this.adapterXmlId = xmlId;
            this.adapter = new DefendSpellbookElementAdapter(
                    spellId,
                    SpellBook.getDefendSpellTemplateById(spellId),
                    parentFragmentManager,
                    parentDialog
            );
        }
    }

    private final List<AdapterHolder> adapterHolders;

    public static DefendSpellbookDialogFragment newInstance(FragmentManager fragmentManager) {
        return new DefendSpellbookDialogFragment(fragmentManager);
    }

    private DefendSpellbookDialogFragment(FragmentManager fragmentManager) {
        adapterHolders = new ArrayList<>();
        List<Integer> defendSpellsXmlIds = List.of(R.id.defend_spells_recycler_view_1, R.id.defend_spells_recycler_view_2, R.id.defend_spells_recycler_view_3);
        List<Integer> defendSpellsIds = SpellBook.getDefendSpellsTemplates().keySet().stream().collect(Collectors.toList());

        for (int defendSpellId : defendSpellsIds) {
            adapterHolders.add(new AdapterHolder(
                defendSpellsXmlIds.get(defendSpellId - 1),
                defendSpellId,
                fragmentManager,
                this
            ));
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

        // redraw
        for (var holder : adapterHolders) {
            setupRecyclerViewAdapter(view, holder.adapterXmlId, holder);
        }

        return view;
    }

    private void initSpellsLayout(LayoutInflater inflater, ViewGroup parentView) {
        // hardcoded intentionally, no time to think of better solution
        DefendSpell spell1 = SpellBook.getDefendSpellTemplateById(1);
        DefendSpell spell2 = SpellBook.getDefendSpellTemplateById(2);
        DefendSpell spell3 = SpellBook.getDefendSpellTemplateById(3);

        if (spell1 != null) {
            createSpellBookCategory(inflater, parentView, R.id.defend_spells_recycler_view_1, spell1.getName());
        }

        if (spell2 != null) {
            createSpellBookCategory(inflater, parentView, R.id.defend_spells_recycler_view_2, spell2.getName());
        }

        if (spell3 != null) {
            createSpellBookCategory(inflater, parentView, R.id.defend_spells_recycler_view_3, spell3.getName());
        }
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

        //DefendSpell defendSpell = SpellBook.getDefendSpellTemplateById(holder.getDefendSpellTemplateId());
        // holder.spells.add(defendSpell);

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
