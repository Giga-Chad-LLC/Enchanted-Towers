package enchantedtowers.client.components.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import enchantedtowers.client.R;
import enchantedtowers.client.components.data.ProtectionWallData;


public class ProtectionWallGridAdapter extends RecyclerView.Adapter<ProtectionWallGridAdapter.ImageViewHolder> {
    @FunctionalInterface
    public interface OnItemClickCallback {
        void call(ProtectionWallData data);
    }

    private final List<ProtectionWallData> items;
    private final OnItemClickCallback callback;

    public ProtectionWallGridAdapter(List<ProtectionWallData> items, OnItemClickCallback callback) {
        this.items = items;
        this.callback = callback;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.protection_wall_item, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ProtectionWallData data = items.get(position);

        holder.imageView.setImageResource(data.getImageId());

        // register click event listener
        holder.imageView.setOnClickListener(v -> callback.call(data));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.protection_wall_image_view);
        }
    }
}
