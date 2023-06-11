package enchantedtowers.client.components.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.locationtech.jts.geom.Envelope;

import java.util.List;

import enchantedtowers.client.R;
import enchantedtowers.client.components.canvas.CanvasDefendSpellDecorator;
import enchantedtowers.client.components.canvas.CanvasWidget;
import enchantedtowers.client.interactors.canvas.CanvasDrawStateInteractor;
import enchantedtowers.game_models.DefendSpell;
import enchantedtowers.game_models.utils.Vector2;

public class DefendSpellbookElementAdapter extends RecyclerView.Adapter<DefendSpellbookElementAdapter.DefendSpellViewHolder> {
    private final List<DefendSpell> defendSpells;

    public DefendSpellbookElementAdapter(List<DefendSpell> defendSpells) {
        this.defendSpells = defendSpells;
    }

    @NonNull
    @Override
    public DefendSpellViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.element_spellbook_item, parent, false);
        CanvasWidget canvas = view.findViewById(R.id.canvas_widget);

        // set new size
        ViewGroup.LayoutParams layoutParams = canvas.getLayoutParams();
        int density = (int)view.getContext().getResources().getDisplayMetrics().density;
        layoutParams.width = 390 * density;
        layoutParams.height = 250 * density;
        canvas.setLayoutParams(layoutParams);

        return new DefendSpellViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DefendSpellViewHolder holder, int index) {
        DefendSpell spell = defendSpells.get(index);
        Envelope boundingBox = spell.getBoundary();

        final double padding_dp = DefendSpellViewHolder.PADDING * holder.density;

        double scaleX = (holder.targetWidth - 2 * padding_dp) / boundingBox.getWidth();
        double scaleY = (holder.targetHeight - 2 * padding_dp) / boundingBox.getHeight();

        double applyingScale = Math.min(scaleX, scaleY);

        DefendSpell scaledSpell = spell.getScaledDefendSpell(applyingScale, applyingScale);
        Envelope scaledBoundingBox = scaledSpell.getBoundary();

        double offsetX = (holder.targetWidth - scaledBoundingBox.getWidth()) / 2.0;
        double offsetY = (holder.targetHeight - scaledBoundingBox.getHeight()) / 2.0;

        holder.canvasWidget.getState().addItem(new CanvasDefendSpellDecorator(
            scaledSpell, new Vector2(offsetX, offsetY)
        ));
        holder.canvasWidget.postInvalidate();
    }

    @Override
    public int getItemCount() {
        return defendSpells.size();
    }

    public static class DefendSpellViewHolder extends RecyclerView.ViewHolder {
        private static final int PADDING = 14;
        private final CanvasWidget canvasWidget;
        private final int targetWidth;
        private final int targetHeight;
        private final double density;

        public DefendSpellViewHolder(@NonNull View itemView) {
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
