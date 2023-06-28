package enchantedtowers.client;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import enchantedtowers.client.components.canvas.CanvasAttackerFragment;
import enchantedtowers.client.components.canvas.CanvasFragment;
import enchantedtowers.client.components.canvas.CanvasProtectorFragment;
import enchantedtowers.client.components.canvas.CanvasSpectatorFragment;
import enchantedtowers.client.components.canvas.CanvasViewingFragment;

public class CanvasActivity extends AppCompatActivity {
    // TODO: rewrite this to a single string
    private boolean isAttacking = false;
    private boolean isSpectating = false;
    private boolean isProtecting = false;
    private boolean isViewing = false;


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        /*
         * If activity is automatically deleted by the system and recreated later,
         * we need to save the `extras` passed to the components when it was created first time
         */
        super.onSaveInstanceState(outState);
        // Save value to the instance state
        outState.putBoolean("isAttacking", isAttacking);
        outState.putBoolean("isSpectating", isSpectating);
        outState.putBoolean("isProtecting", isProtecting);
        outState.putBoolean("isViewing", isViewing);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canvas);

        System.out.println("Load canvas fragment to the canvas activity");

        Bundle extras;
        if (savedInstanceState != null) {
            extras = savedInstanceState;
        }
        else {
           extras = getIntent().getExtras();
        }

        CanvasFragment canvasFragment = null;

        if (extras != null) {
            if (extras.getBoolean("isAttacking", false)) {
                System.out.println("Attacking on canvas");
                isAttacking = true;
                canvasFragment = CanvasAttackerFragment.newInstance();
            } else if (extras.getBoolean("isSpectating", false)) {
                System.out.println("Spectating on canvas");
                isSpectating = true;
                canvasFragment = CanvasSpectatorFragment.newInstance();
            } else if (extras.getBoolean("isProtecting", false)) {
                System.out.println("Protecting on canvas");
                isProtecting = true;
                canvasFragment = CanvasProtectorFragment.newInstance();
            }
            else if (extras.getBoolean("isViewing", false)) {
                System.out.println("Viewing on canvas");
                isViewing = true;
                canvasFragment = CanvasViewingFragment.newInstance();
            }
            else {
                System.out.println("No actions on canvas");
            }
        }
        else {
            System.out.println("No actions on canvas");
        }

        if (canvasFragment == null) {
            throw new RuntimeException("CanvasFragment was not provided with correct type");
        }

        // mount fragment into layout
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.canvas_frame_layout, canvasFragment)
                .commit();
    }
}
