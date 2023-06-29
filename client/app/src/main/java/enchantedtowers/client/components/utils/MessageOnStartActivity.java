package enchantedtowers.client.components.utils;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public interface MessageOnStartActivity {
    default void showOnStartMessageIfExists(Activity currentActivity) {
        Bundle extras = currentActivity.getIntent().getExtras();
        System.out.println(currentActivity.getComponentName() + " extras: " + extras);

        if (extras != null && extras.getBoolean("showToastOnStart", false)) {
            ClientUtils.showToastOnUIThread(currentActivity, extras.getString("toastMessage", ""), Toast.LENGTH_SHORT);
        }
    }
}
