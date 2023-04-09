package enchantedtowers.game_models.utils;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    // returns new list of points that are relative to their bounding-box
    static public List<Vector2> getNormalizedPoints(List<Vector2> points, Vector2 offset) {
        List<Vector2> translatedPoints = new ArrayList<>(points);

        // translate each point
        for (Vector2 p : translatedPoints) {
            p.move(-offset.x, -offset.y);
        }

        return translatedPoints;
    }

    // The condition is required for the correct metric distance calculation
    static public boolean isValidPath(List<Vector2> pathPoints) {
        if (pathPoints.size() < 2 || (pathPoints.size() == 2 && pathPoints.get(0).equals(pathPoints.get(1)))) {
            return false;
        }
        return true;
    }
}
