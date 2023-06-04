package enchantedtowers.client;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import enchantedtowers.client.components.canvas.CanvasAttackerFragment;
import enchantedtowers.client.components.canvas.CanvasFragment;
import enchantedtowers.client.components.canvas.CanvasProtectorFragment;
import enchantedtowers.client.components.canvas.CanvasSpectatorFragment;
import enchantedtowers.client.components.canvas.CanvasViewingFragment;
import enchantedtowers.client.components.fs.AndroidFileReader;
import enchantedtowers.game_logic.EnchantmetTemplatesProvider;
import enchantedtowers.game_models.SpellBook;

public class CanvasActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canvas);

        if (!SpellBook.isInstantiated()) {
            try {
                List<EnchantmetTemplatesProvider.SpellTemplateData> data = EnchantmetTemplatesProvider.parseJson(
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
        Bundle extras = getIntent().getExtras();
        CanvasFragment canvasFragment;

        if (extras != null) {
            if (extras.getBoolean("isAttacking", false)) {
                System.out.println("Attacking on canvas");
                canvasFragment = CanvasAttackerFragment.newInstance();
            } else if (extras.getBoolean("isSpectating", false)) {
                System.out.println("Spectating on canvas");
                canvasFragment = CanvasSpectatorFragment.newInstance();
            } else if (extras.getBoolean("isProtecting", false)) {
                System.out.println("Protecting on canvas");
                canvasFragment = CanvasProtectorFragment.newInstance();
            }
            else if (extras.getBoolean("isViewing", false)) {
                System.out.println("Viewing on canvas");
                canvasFragment = CanvasViewingFragment.newInstance();
            }
            else {
                System.out.println("No actions on canvas");
                canvasFragment = CanvasProtectorFragment.newInstance();
            }
        }
        else {
            System.out.println("No actions on canvas");
            canvasFragment = CanvasFragment.newInstance();
        }

        // mount fragment into layout
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.canvas_frame_layout, canvasFragment)
                .commit();
    }
}
