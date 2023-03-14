package enchantedtowers.client.components.enchantment;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;

import java.util.List;


public class Enchantment {
    // must be relative to the bounded box of path
    // format [ x_0, y_0, x_1, y_1, ... ]
    private float[] points;
    // specifies offset for drawing path
    private PointF offset = new PointF(0f, 0f);

    public Enchantment(Enchantment that) {
        setPoints(that.points);
        setOffset(that.offset);
    }

    public Enchantment(List<PointF> points, PointF offset) {
        setPoints(points);
        setOffset(offset);
    }

    public Enchantment(List<PointF> points) {
        setPoints(points);
    }

    public int getPointsCount() {
        return points.length / 2;
    }

    public PointF getPointAt(int index) {
        return new PointF(points[2 * index], points[2 * index + 1]);
    }

    public float[] getPoints() {
        return points.clone();
    }

    public PointF getOffset() {
        return offset;
    }

    public Path getPath() {
        Path path = new Path();

        if (points.length != 0) {
            path.moveTo(points[0], points[1]);

            for (int i = 2; i < points.length; i += 2) {
                path.lineTo(points[i], points[i + 1]);
            }
        }

        Matrix mat = new Matrix();
        mat.setTranslate(offset.x, offset.y);
        path.transform(mat);

        return path;
    }

    public float[] getScaledPoints(float scaleX, float scaleY, float originX, float originY) {
        Matrix mat = new Matrix();
        float[] dst = new float[points.length];
        mat.setScale(scaleX, scaleY, originX, originY);
        mat.mapPoints(dst, this.points);

        return dst;
    }

    public void setOffset(PointF offset) {
        this.offset.x = offset.x;
        this.offset.y = offset.y;
    }

    private void setPoints(List<PointF> points) {
        this.points = new float[points.size() * 2];
        for (int i = 0; i < points.size(); i++) {
            PointF point = points.get(i);
            this.points[2 * i] = point.x;
            this.points[2 * i + 1] = point.y;
        }
    }

    private void setPoints(float[] points) {
        this.points = points.clone();
    }
}