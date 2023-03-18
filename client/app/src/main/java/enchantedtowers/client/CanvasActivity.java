package enchantedtowers.client;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import enchantedtowers.client.components.enchantment.SpellBook;
import enchantedtowers.client.components.fs.EnchantmetTemplatesFileReader;
import enchantedtowers.game_models.utils.Point;

public class CanvasActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canvas);

        try {
            EnchantmetTemplatesFileReader reader = new EnchantmetTemplatesFileReader(getBaseContext());
            List<List<Point>> data = reader.processFile(R.raw.canvas_templates_config);
            SpellBook.instantiate(data);
        } catch (JSONException | IOException e) {
            Log.e("JSON-CONFIG", e.getMessage());
            System.err.println(e.getMessage());
        }
    }
}
