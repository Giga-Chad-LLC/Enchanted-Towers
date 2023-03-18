package enchantedtowers.client.components.spell;

import org.locationtech.jts.geom.Geometry;

public interface CurvesMatchingMetric {
    float calculate(Geometry template, Geometry pattern);
}
