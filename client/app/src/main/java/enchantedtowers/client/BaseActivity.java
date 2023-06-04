package enchantedtowers.client;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import enchantedtowers.client.components.utils.ClientUtils;

/**
 * This class is required for printing extra messages passed between activities.
 */
public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();

        if (extras != null && extras.getBoolean("showToastOnStart", false)) {
            String message = extras.getString("toastMessage", "");

            if (!message.isEmpty()) {
                ClientUtils.showInfo(this, message);
            }

            // removing extras
            getIntent().putExtra("showToastOnStart", false);
            getIntent().putExtra("toastMessage", "");
        }
    }

    @Override
    public void onDestroy() {
        // dismissing all created notification dialogs
        ClientUtils.dismissCreatedDialogs();
        super.onDestroy();
    }
}
