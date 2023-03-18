package enchantedtowers.client;

import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import enchantedtowers.client.components.enchantment.EnchantmentBook;
import enchantedtowers.client.components.fs.EnchantmetTemplatesFileReader;

public class CanvasActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canvas);

        try {
            EnchantmetTemplatesFileReader reader = new EnchantmetTemplatesFileReader(getBaseContext());
            List<List<PointF>> data = reader.processFile(R.raw.canvas_templates_config);
            EnchantmentBook.instantiate(data);
        }
        catch(JSONException | IOException e) {
            Log.e("JSON-CONFIG", e.getMessage());
            System.err.println(e.getMessage());
        }
    }
}
