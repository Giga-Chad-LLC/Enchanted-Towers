package enchantedtowers.game_logic;

import org.locationtech.jts.geom.Geometry;

public interface CurvesMatchingMetric {

    /**
     * Computes the similarity measure between two geometries in range [0, 1] (the greater the value, the more similar geometries are)
     */
    float calculate(Geometry template, Geometry pattern);
}
