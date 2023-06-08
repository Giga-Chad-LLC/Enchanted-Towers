package enchantedtowers.client.components.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.locationtech.jts.geom.Envelope;

import java.util.List;

import enchantedtowers.client.R;
import enchantedtowers.client.components.canvas.CanvasSpellDecorator;
import enchantedtowers.client.components.canvas.CanvasWidget;
import enchantedtowers.client.interactors.canvas.CanvasDrawStateInteractor;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.utils.Vector2;

public class SpellbookElementAdapter extends RecyclerView.Adapter<SpellbookElementAdapter.SpellViewHolder> {
    private final List<Spell> spells;

    public SpellbookElementAdapter(List<Spell> spells) {
        this.spells = spells;
    }

    @NonNull
    @Override
    public SpellbookElementAdapter.SpellViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.element_spellbook_item, parent, false);
        return new SpellbookElementAdapter.SpellViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SpellbookElementAdapter.SpellViewHolder holder, int index) {
        Spell spell = spells.get(index);
        Envelope boundingBox = spell.getBoundary();

        final double padding_dp = SpellViewHolder.PADDING * holder.density;

        double scaleX = (holder.targetWidth - 2 * padding_dp) / boundingBox.getWidth();
        double scaleY = (holder.targetHeight - 2 * padding_dp) / boundingBox.getHeight();

        double applyingScale = Math.min(scaleX, scaleY);

        Spell scaledSpell = spell.getScaledSpell(applyingScale, applyingScale,0, 0);
        Envelope scaledBoundingBox = scaledSpell.getBoundary();

        double offsetX = (holder.targetWidth - scaledBoundingBox.getWidth()) / 2.0;
        double offsetY = (holder.targetHeight - scaledBoundingBox.getHeight()) / 2.0;

        scaledSpell.setOffset(new Vector2(offsetX, offsetY));

        holder.canvasWidget.getState().addItem(new CanvasSpellDecorator(scaledSpell));
        holder.canvasWidget.postInvalidate();
    }

    @Override
    public int getItemCount() {
        return spells.size();
    }

    public static class SpellViewHolder extends RecyclerView.ViewHolder {
        private static final int PADDING = 14;
        private final CanvasWidget canvasWidget;
        private final int targetWidth;
        private final int targetHeight;
        private final double density;

        public SpellViewHolder(@NonNull View itemView) {
            super(itemView);
            // required to determine width and height of the item component
            itemView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

            targetWidth = itemView.getMeasuredWidth();
            targetHeight = itemView.getMeasuredHeight();

            density = itemView.getContext().getResources().getDisplayMetrics().density;

            canvasWidget = itemView.findViewById(R.id.canvas_widget);
            canvasWidget.setInteractors(List.of(new CanvasDrawStateInteractor()));
        }
    }
}
