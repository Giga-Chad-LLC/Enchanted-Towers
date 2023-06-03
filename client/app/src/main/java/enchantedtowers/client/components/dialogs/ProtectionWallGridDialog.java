package enchantedtowers.client.components.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import enchantedtowers.client.R;
import enchantedtowers.client.components.adapters.ProtectionWallGridAdapter;
import enchantedtowers.client.components.data.ProtectionWallData;
import enchantedtowers.game_models.ProtectionWall;


public class ProtectionWallGridDialog extends Dialog {
    private final List<ProtectionWallData> items;
    private final ProtectionWallGridAdapter adapter;

    public static ProtectionWallGridDialog newInstance(@NonNull Context context, ProtectionWallGridAdapter.OnItemClickCallback callback) {
        return new ProtectionWallGridDialog(context, callback);
    }

    private ProtectionWallGridDialog(Context context, ProtectionWallGridAdapter.OnItemClickCallback callback) {
        super(context);
        items = new ArrayList<>();
        adapter = new ProtectionWallGridAdapter(items, callback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.protection_wall_grid_dialog);

        RecyclerView recyclerView = findViewById(R.id.protection_wall_grid_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(adapter);
    }

    public void addImage(int towerId, int protectionWallId, Integer imageId, String title) {
        int position = items.size();
        items.add(new ProtectionWallData(towerId, protectionWallId, imageId, title));
        adapter.notifyItemInserted(position);
    }
}
