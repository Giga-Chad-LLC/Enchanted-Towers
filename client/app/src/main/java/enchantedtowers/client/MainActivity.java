package enchantedtowers.client;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import enchantedtowers.client.components.fs.AndroidFileReader;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.game_logic.EnchantmetTemplatesProvider;
import enchantedtowers.game_models.SpellBook;


public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: temporary solution (must be done after registration/login)
        Button button = findViewById(R.id.main_activity_set_player_id_button);
        EditText playerIdTextInput = findViewById(R.id.main_activity_player_id_text_input);
        button.setOnClickListener(v -> {
            try {
                int playerId = Integer.parseInt(playerIdTextInput.getText().toString());
                ClientStorage.getInstance().setPlayerId(playerId);

                ClientUtils.showSnackbar(playerIdTextInput, "Player id set to " + playerId, Snackbar.LENGTH_SHORT);
            } catch (NumberFormatException err) {
                ClientUtils.showSnackbar(playerIdTextInput, err.getMessage(), Snackbar.LENGTH_SHORT);
            }
        });
    }

    public void changeActivity(View view) {
        if (view.getId() == R.id.changeToAttackTowerMenu) {
            Intent intent = new Intent(MainActivity.this, AttackTowerMenuActivity.class);
            startActivity(intent);
        }
        else if (view.getId() == R.id.changeToCanvasActivity) {
            Intent intent = new Intent(MainActivity.this, CanvasActivity.class);
            // intent.putExtra("isAttacking", true);
            startActivity(intent);
        }
        else if (view.getId() == R.id.changeToMapActivity) {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);
        }
        else if (view.getId() == R.id.signUp) {
            Intent intent = new Intent(MainActivity.this, UserRegistrationActivity.class);
            startActivity(intent);
        }
        else if (view.getId() == R.id.logIn){
            Intent intent = new Intent(MainActivity.this, UserLoginActivity.class);
            startActivity(intent);
        } else {
            System.err.println("Unknown view emitted `MainActivity::changeActivity`: " + view);
        }
    }
}
