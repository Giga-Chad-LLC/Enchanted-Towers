package enchantedtowers.client.components.enchantment;

public class HausdorffMetric implements CurvesMatchingMetric {
    @Override
    public float calculate(float[] templatePoints, float[] patternPoints) {
        return Math.max(
            calculateMaximumForSets(templatePoints, patternPoints),
            calculateMaximumForSets(patternPoints, templatePoints)
        );
    }

    private float calculateMaximumForSets(float[] points, float[] fixedPoints) {
        float result = 0f;

        for (int i = 0; i < fixedPoints.length; i += 2) {
            float x = fixedPoints[i];
            float y = fixedPoints[i + 1];

            result = Math.max(
                result,
                calculateMinimumForPoint(x, y, points)
            );
        }

        return result;
    }

    private float calculateMinimumForPoint(float x, float y, float[] fixedPoints) {
        float result = Float.POSITIVE_INFINITY;

        for (int i = 0; i < fixedPoints.length; i += 2) {
            float otherX = fixedPoints[i];
            float otherY = fixedPoints[i];

            result = Math.min(
                result,
                getDistanceBetweenPoints(x, y, otherX, otherY)
            );
        }

        return result;
    }

    private float getDistanceBetweenPoints(float x0, float y0, float x1, float y1) {
        double a = x0 - x1;
        double b = y0 - y1;

        return (float)Math.sqrt(
            a * a + b * b
        );
    }
}
