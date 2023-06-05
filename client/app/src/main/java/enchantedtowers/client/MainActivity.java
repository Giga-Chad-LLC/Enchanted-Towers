package enchantedtowers.client;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.opencv.android.OpenCVLoader;

import java.io.IOException;
import java.util.List;

import enchantedtowers.client.components.fs.AndroidFileReader;
import enchantedtowers.game_logic.json.DefendSpellsTemplatesProvider;
import enchantedtowers.game_logic.json.SpellsTemplatesProvider;
import enchantedtowers.game_models.SpellBook;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize spell book
        if (!SpellBook.isInstantiated()) {
            try {
                String jsonConfig = AndroidFileReader.readRawFile(getBaseContext(), R.raw.canvas_templates_config);
                List<SpellsTemplatesProvider.SpellTemplateData> spellsData = SpellsTemplatesProvider.parseSpellsJson(jsonConfig);
                List<DefendSpellsTemplatesProvider.DefendSpellTemplateData> defendSpellsData = DefendSpellsTemplatesProvider.parseDefendSpellsJson(jsonConfig);
                SpellBook.instantiate(spellsData, defendSpellsData);
            } catch (JSONException | IOException e) {
                Log.e("JSON-CONFIG", e.getMessage());
                System.err.println(e.getMessage());
            }
        }

        // initialize opencv
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Unable to load OpenCV!");
        }
        else {
            Log.d("OpenCV", "OpenCV loaded Successfully!");
        }
    }

    public void changeActivity(View view) {
        if (view.getId() == R.id.changeToCanvasActivity) {
            Intent intent = new Intent(MainActivity.this, CanvasActivity.class);
            intent.putExtra("isAttacking", true);
            startActivity(intent);
        }
        else if (view.getId() == R.id.changeToMapActivity) {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);
        }
        else if (view.getId() == R.id.changeToAttackTowerMenu) {
            Intent intent = new Intent(MainActivity.this, AttackTowerMenuActivity.class);
            startActivity(intent);
        }
        else if (view.getId() == R.id.changeToCameraActivity) {
            Intent intent = new Intent(MainActivity.this, CastDefendSpellActivity.class);
            startActivity(intent);
        }
        else {
            System.err.println("Unknown view emitted `MainActivity::changeActivity`: " + view);
        }
    }
}
