package enchantedtowers.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.Random;

import enchantedtowers.game_logic.CurvesMatchingMetric;
import enchantedtowers.game_logic.HausdorffMetric;

public class CurvesMetricUnitTest {
    @Test
    public void distanceFromItself_isZero() {
        CurvesMatchingMetric metric = new HausdorffMetric();
        int tests = 100;
        double EPS = 1e-10;
        GeometryFactory factory = new GeometryFactory();

        for (int i = 1; i <= tests; ++i) {
            int size = 2 * generateRandomInt(50, 150);
            float[] points = generateRandomFloatArray(size, 0f, 300f);
            Coordinate[] coords = new Coordinate[points.length / 2];

            for (int j = 0; j < coords.length; j++) {
                coords[j] = new Coordinate(
                        points[2 * j],
                        points[2 * j + 1]
                );
            }


            // must be exact zero
            Geometry g1 = factory.createLineString(coords);
            Geometry g2 = factory.createLineString(coords);
            assertEquals(metric.calculate(g1, g2), 1.0f, EPS);
        }
    }

    private float[] generateRandomFloatArray(int size, float mn, float mx) {
        float[] array = new float[size];

        for (int i = 0; i < size; i++) {
            array[i] = generateRandomFloat(mn, mx);
        }

        return array;
    }


    // returns number in range [mn, mx]
    private float generateRandomFloat(float mn, float mx) {
        return (float) (mn + Math.random() * (mx - mn));
    }

    // returns number in range [mn, mx]
    private int generateRandomInt(int mn, int mx) {
        return (int) (mn + Math.random() * (mx - mn + 1));
    }
}
