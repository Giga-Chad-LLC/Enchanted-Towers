package enchantedtowers.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class UserRegistrationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
    }

    public void sendUserData(View view) {
        if (view.getId() == R.id.buttonToSendData) {
            System.err.println("implement button");
        } else {
            System.err.println("Unknown view emitted `MainActivity::changeActivity`: " + view);
        }
    }
}
