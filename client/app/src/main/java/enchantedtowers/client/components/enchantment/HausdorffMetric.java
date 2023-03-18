package enchantedtowers.client.components.enchantment;

import org.locationtech.jts.algorithm.match.HausdorffSimilarityMeasure;
import org.locationtech.jts.geom.Geometry;

public class HausdorffMetric implements CurvesMatchingMetric {
    @Override
    public float calculate(Geometry template, Geometry pattern) {
        return (float) (new HausdorffSimilarityMeasure()).measure(template, pattern);
    }
}
