package enchantedtowers.client.components.dialogs;

import static enchantedtowers.game_logic.json.SpellsTemplatesProvider.EARTH_SPELL_LABEL;
import static enchantedtowers.game_logic.json.SpellsTemplatesProvider.FIRE_SPELL_LABEL;
import static enchantedtowers.game_logic.json.SpellsTemplatesProvider.WATER_SPELL_LABEL;
import static enchantedtowers.game_logic.json.SpellsTemplatesProvider.WIND_SPELL_LABEL;

import android.annotation.SuppressLint;
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
import java.util.stream.Collectors;

import enchantedtowers.client.R;
import enchantedtowers.client.components.adapters.SpellbookElementAdapter;
import enchantedtowers.common.utils.proto.common.SpellType;
import enchantedtowers.game_logic.json.SpellsTemplatesProvider;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellBook;

public class SpellbookDialogFragment extends DialogFragment {
    private static class AdapterHolder {
        private final List<Spell> spells;
        private final SpellbookElementAdapter adapter;

        AdapterHolder(List<Spell> spells) {
            this.spells = spells;
            this.adapter = new SpellbookElementAdapter(spells);
        }
    }

    private final AdapterHolder fireAdapterHolder;
    private final AdapterHolder windAdapterHolder;
    private final AdapterHolder earthAdapterHolder;
    private final AdapterHolder waterAdapterHolder;

    public static SpellbookDialogFragment newInstance() {
        return new SpellbookDialogFragment();
    }

    private SpellbookDialogFragment() {
        fireAdapterHolder = new AdapterHolder(new ArrayList<>());
        windAdapterHolder = new AdapterHolder(new ArrayList<>());
        earthAdapterHolder = new AdapterHolder(new ArrayList<>());
        waterAdapterHolder = new AdapterHolder(new ArrayList<>());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_spellbook_dialog, container, false);

        // Customize and setup the dialog window
        // set title
        ((TextView)view.findViewById(R.id.spellbook_dialog_title)).setText("Spells");
        // populate the spells
        initSpellsLayout(inflater, (ViewGroup)view);

        Dialog dialog = getDialog();

        if (dialog != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // setting close button
        Button closeButton = view.findViewById(R.id.close_spellbook_button);
        closeButton.setOnClickListener(v -> dismiss());

        // clearing drawn spells
        List.of(fireAdapterHolder.spells, windAdapterHolder.spells, earthAdapterHolder.spells, waterAdapterHolder.spells)
                .forEach(List::clear);

        // fire
        setupRecyclerViewAdapter(view, R.id.fire_spells_recycler_view, SpellType.FIRE_SPELL, fireAdapterHolder);
        // wind
        setupRecyclerViewAdapter(view, R.id.wind_spells_recycler_view, SpellType.WIND_SPELL, windAdapterHolder);
        // earth
        setupRecyclerViewAdapter(view, R.id.earth_spells_recycler_view, SpellType.EARTH_SPELL, earthAdapterHolder);
        // water
        setupRecyclerViewAdapter(view, R.id.water_spells_recycler_view, SpellType.WATER_SPELL, waterAdapterHolder);

        return view;
    }

    private void initSpellsLayout(LayoutInflater inflater, ViewGroup parentView) {
        createSpellBookCategory(inflater, parentView, R.id.fire_spells_recycler_view, FIRE_SPELL_LABEL);
        createSpellBookCategory(inflater, parentView, R.id.wind_spells_recycler_view, WIND_SPELL_LABEL);
        createSpellBookCategory(inflater, parentView, R.id.earth_spells_recycler_view, EARTH_SPELL_LABEL);
        createSpellBookCategory(inflater, parentView, R.id.water_spells_recycler_view, WATER_SPELL_LABEL);
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

    private void setupRecyclerViewAdapter(View view, Integer recyclerViewId, SpellType targetSpellType, AdapterHolder holder) {
        RecyclerView recyclerView = view.findViewById(recyclerViewId);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(holder.adapter);

        List<Spell> matchingSpells = SpellBook.getSpellTemplates().values().stream()
                .filter(spell -> targetSpellType == spell.getSpellType()).collect(Collectors.toList());

        holder.spells.addAll(matchingSpells);
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
