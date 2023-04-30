package enchantedtowers.game_models;

import java.util.ArrayList;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.util.AffineTransformation;

import java.util.List;

import enchantedtowers.game_models.utils.Vector2;


public class Spell {
    // must be relative to the bounded box of path
    private Geometry curve;
    // specifies offset for drawing path
    private final Vector2 offset = new Vector2(0, 0);

    public Spell(Spell that) {
        curve = that.curve.copy();
        setOffset(that.offset);
    }

    public Spell(List<Vector2> points, Vector2 offset) {
        setPoints(points);
        setOffset(offset);
    }

    public Spell(List<Vector2> points) {
        setPoints(points);
    }

    public Envelope getBoundary() {
        return curve.getEnvelopeInternal();
    }

    public int getPointsCount() {
        return curve.getNumPoints();
    }

    public Vector2 getPointAt(int index) {
        Coordinate point = curve.getCoordinates()[index];
        return new Vector2(point.getX(), point.getY());
    }

    public Vector2 getOffset() {
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

    public void setOffset(Vector2 offset) {
        this.offset.x = offset.x;
        this.offset.y = offset.y;
    }

    private void setPoints(List<Vector2> points) {
        double[] pointsCopy = new double[points.size() * 2];

        for (int i = 0; i < points.size(); i++) {
            Vector2 point = points.get(i);
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

    public List<Vector2> getPointsList() {
        List<Vector2> pointsList = new ArrayList<>();
        Coordinate[] points = curve.getCoordinates();

        for (Coordinate point : points) {
            pointsList.add(new Vector2(
                point.getX(),
                point.getY()
            ));
        }

        return pointsList;
    }
}