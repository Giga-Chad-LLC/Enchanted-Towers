package enchantedtowers.game_models;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.util.AffineTransformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import enchantedtowers.game_models.utils.Vector2;

public class DefendSpell {
    public enum DefendSpellType {
        INVERT_X_AXIS(1),
        INVERT_Y_AXIS(2),
        VIBRATE(3);

        private final int type; // corresponds to the id of the defend spell
        DefendSpellType(int type) {
            this.type = type;
        }
        public int getType() {
            return this.type;
        }
    };

    // must be relative to the bounded box of path
    // (meaning when drawn offset must be applied, defend spells must be drawn on the center of canvas)
    private Geometry curvesUnion;
    private final String name;
    private final List<List<Vector2>> lines; // points are normalized


    public DefendSpell(String name, List<List<Vector2>> lines) {
        this.name = name;
        this.lines = lines;
        setCurveUnion(lines);
    }

    public DefendSpell(List<List<Vector2>> lines) {
        this.name = "unknown";
        this.lines = lines;
        setCurveUnion(lines);
    }

    public DefendSpell(DefendSpell that) {
        curvesUnion = that.curvesUnion.copy();
        name = that.name;

        lines = new ArrayList<>();
        for (var line : that.lines) {
            lines.add(new ArrayList<>(line));
        }
    }

    public String getName() {
        return name;
    }

    public List<List<Vector2>> getPoints() {
        return Collections.unmodifiableList(lines);
    }

    public Envelope getBoundary() {
        return curvesUnion.getEnvelopeInternal();
    }

    public Geometry getScaledCurve(double scaleX, double scaleY, double originX, double originY) {
        Geometry geometry = curvesUnion.copy();
        geometry.apply(
                AffineTransformation.scaleInstance(scaleX, scaleY, originX, originY)
        );
        return geometry;
    }

    /**
     * Relies on the invariant that {@code this.lines} points are normalized
     * @param scaleX scaling factor in X axis
     * @param scaleY scaling factor in Y axis
     * @return new instance of scaled {@code DefendSpell} object
     */
    public DefendSpell getScaledDefendSpell(double scaleX, double scaleY) {
        List<List<Vector2>> newLines = new ArrayList<>();

        for (var line : lines) {
            List<Vector2> newLine = new ArrayList<>();

            for (var p : line) {
                newLine.add(new Vector2(p.x * scaleX, p.y * scaleY));
            }

            newLines.add(newLine);
        }

        return new DefendSpell(newLines);
    }

    private List<List<Vector2>> getLinesList(List<Vector2> mergedPoints) {
        List<List<Vector2>> lines = new ArrayList<>();
        List<Vector2> currentLine = new ArrayList<>();

        int currentLineIndex = 0;
        for (Vector2 p : mergedPoints) {
            if (this.lines.get(currentLineIndex).size() == currentLine.size()) {
                lines.add(currentLine);
                currentLine = new ArrayList<>();
                currentLineIndex++;
            }

            currentLine.add(p);
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine);
        }

        return lines;
    }

    private List<Vector2> getPointsList(Geometry geometry) {
        List<Vector2> pointsList = new ArrayList<>();
        Coordinate[] points = geometry.getCoordinates();

        for (Coordinate point : points) {
            pointsList.add(new Vector2(
                    point.getX(),
                    point.getY()
            ));
        }

        return pointsList;
    }

    public Geometry getCurveUnionCopy() {
        return curvesUnion.copy();
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
