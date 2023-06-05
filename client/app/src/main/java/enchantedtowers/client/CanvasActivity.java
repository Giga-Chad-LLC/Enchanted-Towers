package enchantedtowers.client;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import enchantedtowers.client.components.canvas.CanvasAttackerFragment;
import enchantedtowers.client.components.canvas.CanvasFragment;
import enchantedtowers.client.components.canvas.CanvasProtectorFragment;
import enchantedtowers.client.components.canvas.CanvasSpectatorFragment;

public class CanvasActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canvas);

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
