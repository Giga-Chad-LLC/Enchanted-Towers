package enchantedtowers.game_logic.algorithm;

import org.locationtech.jts.algorithm.match.HausdorffSimilarityMeasure;
import org.locationtech.jts.geom.Geometry;

public class HausdorffMetric implements CurvesMatchingMetric {
    private final HausdorffSimilarityMeasure hausdorffSimilarityMeasure = new HausdorffSimilarityMeasure();

    @Override
    public float calculate(Geometry template, Geometry pattern) {
        return (float) hausdorffSimilarityMeasure.measure(template, pattern);
    }
}
