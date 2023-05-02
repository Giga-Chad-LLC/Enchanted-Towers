package enchantedtowers.client.components.utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.RectF;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Optional;

import enchantedtowers.client.AttackTowerMenuActivity;
import enchantedtowers.game_models.utils.Vector2;

public class AndroidUtils {
    public static Vector2 getPathOffset(Path path) {
        // calculate bounding box for the path
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);

        return new Vector2(bounds.left, bounds.top);
    }

    public static void showToastOnUIThread(Activity context, String message, int type) {
        context.runOnUiThread(() -> Toast.makeText(context, message, type).show());
    }

    /**
     * Goes back in activity history and removes all activities that do not match the {@code AttackTowerMenuActivity} class.
     */
    public static void redirectToActivityAndPopHistory(Activity from, Class<?> to, String message) {
        Intent intent = new Intent(from, to);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (message != null) {
            intent.putExtra("showToastOnStart", true);
            intent.putExtra("toastMessage", message);
        }

        System.out.println("redirectToBaseActivity(): from=" + from + ", to=" + to + ", intent=" + intent);
        from.startActivity(intent);
    }
}
