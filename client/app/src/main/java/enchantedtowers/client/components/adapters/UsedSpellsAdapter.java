package enchantedtowers.client.components.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import enchantedtowers.client.R;

public class UsedSpellsAdapter extends RecyclerView.Adapter<UsedSpellsAdapter.ImageViewHolder> {
    private final List<Integer> spellImageIds;

    public UsedSpellsAdapter(List<Integer> spellImageIds) {
        this.spellImageIds = spellImageIds;
    }

    @NonNull
    @Override
    public UsedSpellsAdapter.ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.used_spell_item, parent, false);
        return new UsedSpellsAdapter.ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsedSpellsAdapter.ImageViewHolder holder, int position) {
        Integer imageId = spellImageIds.get(position);
        holder.imageView.setImageResource(imageId);
    }

    @Override
    public int getItemCount() {
        return spellImageIds.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.used_spell_image_view);
        }
    }
}
