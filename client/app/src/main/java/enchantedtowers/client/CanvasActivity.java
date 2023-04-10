package enchantedtowers.client;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import enchantedtowers.client.components.canvas.CanvasFragment;
import enchantedtowers.client.components.fs.AndroidFileReader;
import enchantedtowers.client.components.map.MapFragment;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_logic.EnchantmetTemplatesProvider;
import enchantedtowers.game_models.SpellTemplate;
import enchantedtowers.game_models.utils.Vector2;

public class CanvasActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canvas);

        if (!SpellBook.isInstantiated()) {
            try {
                List<SpellTemplate> data = EnchantmetTemplatesProvider.parseJson(
                        AndroidFileReader.readRawFile(getBaseContext(), R.raw.canvas_templates_config)
                );
                SpellBook.instantiate(data);
            } catch (JSONException | IOException e) {
                Log.e("JSON-CONFIG", e.getMessage());
                System.err.println(e.getMessage());
            }
        }

        System.out.println("Load canvas fragment to the canvas activity");
        // create fragment
        Fragment canvasFragment = CanvasFragment.newInstance();

        // mount fragment into layout
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.canvas_frame_layout, canvasFragment)
                .commit();
    }
}
