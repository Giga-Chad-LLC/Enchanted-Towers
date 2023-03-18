package enchantedtowers.client.components.enchantment;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.util.AffineTransformation;

import java.util.List;

import enchantedtowers.game_models.utils.Point;


public class Spell {
    // must be relative to the bounded box of path
    public Geometry curve;
    // specifies offset for drawing path
    private final Point offset = new Point(0, 0);

    public Spell(Spell that) {
        curve = that.curve.copy();
        setOffset(that.offset);
    }

    public Spell(List<Point> points, Point offset) {
        setPoints(points);
        setOffset(offset);
    }

    public Spell(List<Point> points) {
        setPoints(points);
    }

    public Envelope getBoundary() {
        return curve.getEnvelopeInternal();
    }

    public int getPointsCount() {
        return curve.getNumPoints();
    }

    public Point getPointAt(int index) {
        Coordinate point = curve.getCoordinates()[index];
        return new Point(point.getX(), point.getY());
    }

    public double[] getPoints() {
        Coordinate[] coordinates = curve.getCoordinates();
        double[] pts = new double[coordinates.length * 2];

        for (int i = 0; i < coordinates.length; i++) {
            pts[2 * i] = coordinates[i].getX();
            pts[2 * i + 1] = coordinates[i].getY();
        }

        return pts;
    }

    public Point getOffset() {
        return offset;
    }

    public Geometry getCurve() {
        return curve.copy();
    }

    public Geometry getScaledCurve(double scaleX, double scaleY, double originX, double originY) {
        Geometry geometry = curve.copy();
        geometry.apply(
                AffineTransformation.scaleInstance(scaleX, scaleY, originX, originY)
        );
        return geometry;
    }

    public void setOffset(Point offset) {
        this.offset.x = offset.x;
        this.offset.y = offset.y;
    }

    private void setPoints(List<Point> points) {
        double[] pointsCopy = new double[points.size() * 2];

        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            pointsCopy[2 * i] = point.x;
            pointsCopy[2 * i + 1] = point.y;
        }

        setPoints(pointsCopy);
    }

    private void setPoints(double[] points) {
        GeometryFactory factory = new GeometryFactory();
        Coordinate[] coordinates = new Coordinate[points.length / 2];

        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = new Coordinate(points[2 * i], points[2 * i + 1]);
        }

        curve = factory.createLineString(coordinates);
    }
}