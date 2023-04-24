package enchantedtowers.game_logic;

import org.locationtech.jts.algorithm.match.HausdorffSimilarityMeasure;
import org.locationtech.jts.geom.Geometry;

import enchantedtowers.game_logic.CurvesMatchingMetric;

public class HausdorffMetric implements CurvesMatchingMetric {
    private final HausdorffSimilarityMeasure hausdorffSimilarityMeasure = new HausdorffSimilarityMeasure();

    @Override
    public float calculate(Geometry template, Geometry pattern) {
        return (float) hausdorffSimilarityMeasure.measure(template, pattern);
    }
}
