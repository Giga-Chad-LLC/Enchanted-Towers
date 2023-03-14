package enchantedtowers.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import enchantedtowers.client.components.enchantment.CurvesMatchingMetric;
import enchantedtowers.client.components.enchantment.HausdorffMetric;

public class CurvesMetricUnitTest {
    @Test
    public void distanceFromItself_isZero() {
        CurvesMatchingMetric metric = new HausdorffMetric();
        int tests = 1000;
        double EPS = 1e-10;

        for (int i = 1; i <= tests; ++i) {
            int size = 2 * generateRandomInt(50, 150);
            float[] points = generateRandomFloatArray(size, 0f, 300f);

            // must be exact zero
            assertEquals(metric.calculate(points, points), 0f, EPS);
        }


        assertEquals(4, 2 + 2);
    }

    private float[] generateRandomFloatArray(int size, float mn, float mx) {
        float[] array = new float[size];

        for (int i = 0; i < size; i++) {
            array[i] = generateRandomFloat(mn, mx);
        }

        return array;
    }


    // returns number in range [mn, mx)
    private float generateRandomFloat(float mn, float mx) {
        return (float) (Math.random() * (mx - mn + 1) + mn);
    }

    // returns number in range [mn, mx)
    private int generateRandomInt(int mn, int mx) {
        return (int) (Math.random() * (mx - mn + 1) + mn);
    }
}
