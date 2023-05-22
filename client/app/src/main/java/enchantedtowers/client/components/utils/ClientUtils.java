package enchantedtowers.client.components.utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.RectF;
import android.widget.Toast;

import java.util.List;

import enchantedtowers.client.components.dialogs.NotificationDialog;
import enchantedtowers.common.utils.proto.common.SpellType;
import enchantedtowers.game_models.utils.Vector2;

public class ClientUtils {
    public static Vector2 getPathOffset(Path path) {
        // calculate bounding box for the path
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);

        return new Vector2(bounds.left, bounds.top);
    }

    public static void showToastOnUIThread(Activity context, String message, int type) {
        context.runOnUiThread(() -> Toast.makeText(context, message, type).show());
    }

    public static void showError(Activity context, String description) {
        showNotificationOnUIThread(context, "Error occurred", description, "Dismiss", null);
    }

    public static void showInfo(Activity context, String description) {
        showNotificationOnUIThread(context, "Event info", description, "Dismiss", null);
    }

    public static void showNotificationOnUIThread(Activity context, String title, String description, String buttonMessage, Runnable callback) {
        context.runOnUiThread(() -> {
            NotificationDialog dialog = NotificationDialog.newInstance(context, title, description, buttonMessage, callback);
            dialog.show();
        });
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

    /**
     * Retrieves actual color by SpellType
     */
    public static int getColorIdBySpellType(SpellType spellType) {
        return switch (spellType) {
            case FIRE_SPELL -> Color.rgb(255, 96, 0);
            case WATER_SPELL -> Color.rgb(4, 135, 217);
            case WIND_SPELL -> Color.rgb(102, 140, 43);
            case EARTH_SPELL -> Color.rgb(64, 37, 14);
            case UNRECOGNIZED -> Color.BLACK;
        };
    }

    public static List<SpellType> getSpellTypesList() {
        return List.of(SpellType.FIRE_SPELL, SpellType.WATER_SPELL, SpellType.WIND_SPELL, SpellType.EARTH_SPELL);
    }
}
