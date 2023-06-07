package components.utils;

import enchantedtowers.game_models.utils.Vector2;

public class MathUtils {
    private MathUtils() {}

    static public boolean isInsideRequiredArea(Vector2 a, Vector2 b, double distance) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return dx * dx + dy * dy <= distance * distance;
    }
}
