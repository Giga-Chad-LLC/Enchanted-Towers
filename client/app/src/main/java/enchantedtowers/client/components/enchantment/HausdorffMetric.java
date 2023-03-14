package enchantedtowers.client.components.enchantment;

import org.locationtech.jts.algorithm.distance.DiscreteHausdorffDistance;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

public class HausdorffMetric implements CurvesMatchingMetric {
    @Override
    public float calculate(float[] template, float[] pattern) {
        GeometryFactory factory = new GeometryFactory();
        Geometry templateString = factory.createLineString(
                getCoordinatesFromRawPoints(template)
        );
        Geometry patternString = factory.createLineString(
                getCoordinatesFromRawPoints(pattern)
        );

        double result = (new DiscreteHausdorffDistance(templateString, patternString)).distance();

        return (float) result;
    }

    private Coordinate[] getCoordinatesFromRawPoints(float[] points) {
        Coordinate[] coordinates = new Coordinate[points.length / 2];

        for (int i = 0; i < coordinates.length; ++i) {
            coordinates[i] = new Coordinate(points[2 * i], points[2 * i + 1]);
        }

        return coordinates;
    }
}
