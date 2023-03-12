package enchantedtowers.client.components.enchantment;

import android.graphics.PointF;

import java.util.ArrayList;


public class Enchantment {
    // must be relative to the bounded box of path
    // format [ x_0, y_0, x_1, y_1, ... ]
    private final float[] points;

    public int getPointsCount() {
        return points.length / 2;
    }

    public PointF getPointAt(int index) {
        return new PointF(points[2 * index], points[2 * index + 1]);
    }

    public Enchantment(ArrayList<PointF> points) {
        this.points = new float[points.size() * 2];
        for (int i = 0; i < points.size(); i++) {
            PointF point = points.get(i);
            this.points[2 * i] = point.x;
            this.points[2 * i + 1] = point.y;
        }
    }
}