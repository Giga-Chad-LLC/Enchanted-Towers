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


public class ProtectionWallGridDialog extends Dialog {
    private final List<ProtectionWallData> items;
    private final ProtectionWallGridAdapter adapter;

    public ProtectionWallGridDialog(@NonNull Context context) {
        super(context);
        items = new ArrayList<>();
        adapter = new ProtectionWallGridAdapter(items);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.protection_wall_grid_dialog);

        RecyclerView recyclerView = findViewById(R.id.protection_wall_grid_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(adapter);
    }

    public void addImage(Integer imageId, String title) {
        int position = items.size();
        items.add(new ProtectionWallData(imageId, title));
        adapter.notifyItemInserted(position);
        // adapter.notifyDataSetChanged();
    }
}
