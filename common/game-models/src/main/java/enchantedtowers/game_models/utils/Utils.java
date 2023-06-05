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

    // returns new list of points that are relative to their bounding-box but does that for more complex structure
    static public List<List<Vector2>> getNormalizedLines(List<List<Vector2>> lines) {
        // make a copy and calculate offset
        List<List<Vector2>> translatedLines = new ArrayList<>(lines.size());
        double offsetX = Float.POSITIVE_INFINITY;
        double offsetY = Float.POSITIVE_INFINITY;

        for (var points : lines) {
            translatedLines.add(new ArrayList<>(points));

            for (var point : points) {
                offsetX = Math.min(offsetX, point.x);
                offsetY = Math.min(offsetY, point.y);
            }
        }


        // translate each point
        for (var points : translatedLines) {
            for (var point : points) {
                point.move(-offsetX, -offsetY);
            }
        }

        return translatedLines;
    }

    // The condition is required for the correct metric distance calculation
    static public boolean isValidPath(List<Vector2> pathPoints) {
        if (pathPoints.size() < 2 || (pathPoints.size() == 2 && pathPoints.get(0).equals(pathPoints.get(1)))) {
            return false;
        }
        return true;
    }
}
