package enchantedtowers.game_models;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import enchantedtowers.game_models.utils.Vector2;

public class DefendSpell {
    // must be relative to the bounded box of path
    // (meaning when drawn offset must be applied, defend spells must be drawn on the center of canvas)
    private Geometry curvesUnion;
    private final List<List<Vector2>> lines;

    public DefendSpell(List<List<Vector2>> lines) {
        this.lines = lines;
        setCurveUnion(lines);
    }

    public List<List<Vector2>> getPoints() {
        return Collections.unmodifiableList(lines);
    }

    private void setCurveUnion(List<List<Vector2>> points) {
        Geometry[] geometries = new Geometry[points.size()];
        GeometryFactory factory = new GeometryFactory();

        int index = 0;
        for (List <Vector2> line : points) {
            Coordinate[] coordinates = new Coordinate[line.size()];

            for (int i = 0; i < coordinates.length; i++) {
                Vector2 p = line.get(i);
                coordinates[i] = new Coordinate(p.x, p.y);
            }

            geometries[index++] = factory.createLineString(coordinates);
        }
        GeometryCollection geometryCollection = new GeometryCollection(geometries, factory);
        curvesUnion = geometryCollection.union();
    }
}
