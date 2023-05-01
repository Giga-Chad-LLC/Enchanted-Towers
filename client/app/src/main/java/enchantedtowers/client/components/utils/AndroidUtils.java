package enchantedtowers.client.components.utils;

import android.graphics.Path;
import android.graphics.RectF;

import enchantedtowers.game_models.utils.Vector2;

public class AndroidUtils {
    public static Vector2 getPathOffset(Path path) {
        // calculate bounding box for the path
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);

        return new Vector2(bounds.left, bounds.top);
    }
}
