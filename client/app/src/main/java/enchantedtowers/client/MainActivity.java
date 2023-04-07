package enchantedtowers.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void changeActivity(View view) {
        if (view.getId() == R.id.changeToCanvasActivity) {
            Intent intent = new Intent(MainActivity.this, CanvasActivity.class);
            startActivity(intent);
        } else if(view.getId() == R.id.changeToMapActivity) {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);
        }
        else {
            System.err.println("Unknown view emitted `MainActivity::changeActivity`: " + view.toString());
        }
    }
}
