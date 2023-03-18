package enchantedtowers.client.interactors.canvas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

import enchantedtowers.client.components.canvas.CanvasSpellDecorator;
import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.client.components.spell.Spell;
import enchantedtowers.client.components.spell.SpellBook;
import enchantedtowers.client.components.spell.SpellsPatternMatchingAlgorithm;
import enchantedtowers.client.components.spell.HausdorffMetric;
import enchantedtowers.game_models.utils.Point;

public class CanvasDrawEnchantmentInteractor implements CanvasInteractor {
    private final Path path = new Path();
    private final List<Point> pathPoints = new ArrayList<>();
    private final Paint brush;

    private boolean isValidPath() {
        // The condition is required for the correct Hausdorff distance calculation
        if (pathPoints.size() < 2 || (pathPoints.size() == 2 && pathPoints.get(0).equals(pathPoints.get(1)))) {
            return false;
        }
        return true;
    }

    public CanvasDrawEnchantmentInteractor(CanvasState state) {
        brush = state.getBrush();
    }

    @Override
    public void onDraw(CanvasState state, Canvas canvas) {
        canvas.drawPath(path, brush);
    }

    @Override
    public boolean onTouchEvent(CanvasState state, float x, float y, int motionEventType) {
        switch (motionEventType) {
            case MotionEvent.ACTION_DOWN: {
                path.moveTo(x, y);
                pathPoints.add(new Point(x, y));
                // update color only when started the new shape
                brush.setColor(state.getBrushColor());

                return true;
            }
            case MotionEvent.ACTION_UP: {
                path.lineTo(x, y);
                pathPoints.add(new Point(x, y));

                if (isValidPath()) {
                    Spell pattern = new Spell(
                            getNormalizedPoints(pathPoints),
                            getPathOffset(path)
                    );

                    Spell matchedSpell = SpellsPatternMatchingAlgorithm.getMatchedTemplate(
                            SpellBook.getTemplates(),
                            pattern,
                            new HausdorffMetric()
                    );

                    if (matchedSpell != null) {
                        CanvasSpellDecorator canvasMatchedEnchantment = new CanvasSpellDecorator(
                                // matchedSpell.getPath(),
                                brush.getColor(),
                                matchedSpell
                        );

                        state.addItem(canvasMatchedEnchantment);
                    }
                }

                path.reset();
                pathPoints.clear();

                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                path.lineTo(x, y);
                pathPoints.add(new Point(x, y));
                return true;
            }
            default: {
                return false;
            }
        }
    }

    // returns new list of points that are relative to their bounding-box
    private List<Point> getNormalizedPoints(
            List<Point> points
    ) {
        Point offset = getPathOffset(path);
        List<Point> translatedPoints = new ArrayList<>(points);

        // translate each point
        for (Point p : translatedPoints) {
            p.move(-offset.x, -offset.y);
        }

        return translatedPoints;
    }

    private Point getPathOffset(Path path) {
        // calculate bounding box for the path
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);

        return new Point(bounds.left, bounds.top);
    }
}