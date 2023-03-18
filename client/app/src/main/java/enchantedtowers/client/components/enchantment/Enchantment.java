package enchantedtowers.client.components.enchantment;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.util.AffineTransformation;

import java.util.List;


public class Enchantment {
    // must be relative to the bounded box of path
    private Geometry curve;
    // specifies offset for drawing path
    private final PointF offset = new PointF(0f, 0f);

    public Enchantment(Enchantment that) {
        curve = that.curve.copy();
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
        return curve.getNumPoints();
    }

    public PointF getPointAt(int index) {
        Coordinate point = curve.getCoordinates()[index];
        return new PointF((float) point.getX(), (float) point.getY());
    }

    public float[] getPoints() {
        Coordinate[] coordinates = curve.getCoordinates();
        float[] pts = new float[coordinates.length * 2];

        for (int i = 0; i < coordinates.length; i++) {
            pts[2 * i] = (float) coordinates[i].getX();
            pts[2 * i + 1] = (float) coordinates[i].getY();
        }

        return pts;
    }

    public PointF getOffset() {
        return offset;
    }

    public Path getPath() {
        Path path = new Path();
        Coordinate[] coordinates = curve.getCoordinates();

        if (coordinates.length != 0) {
            path.moveTo((float) coordinates[0].getX(), (float) coordinates[0].getY());

            for (int i = 1; i < coordinates.length; i++) {
                path.lineTo((float) coordinates[i].getX(), (float) coordinates[i].getY());
            }
        }

        Matrix mat = new Matrix();
        mat.setTranslate(offset.x, offset.y);
        path.transform(mat);

        return path;
    }

    public Geometry getCurve() {
        return curve.copy();
    }

    public Geometry getScaledCurve(float scaleX, float scaleY, float originX, float originY) {
        Geometry geometry = curve.copy();
        geometry.apply(
                AffineTransformation.scaleInstance(scaleX, scaleY, originX, originY)
        );
        return geometry;
    }

    public void setOffset(PointF offset) {
        this.offset.x = offset.x;
        this.offset.y = offset.y;
    }

    private void setPoints(List<PointF> points) {
        float[] pointsCopy = new float[points.size() * 2];

        for (int i = 0; i < points.size(); i++) {
            PointF point = points.get(i);
            pointsCopy[2 * i] = point.x;
            pointsCopy[2 * i + 1] = point.y;
        }

        setPoints(pointsCopy);
    }

    private void setPoints(float[] points) {
        GeometryFactory factory = new GeometryFactory();
        Coordinate[] coordinates = new Coordinate[points.length / 2];

        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = new Coordinate(points[2 * i], points[2 * i + 1]);
        }

        curve = factory.createLineString(coordinates);
    }
}