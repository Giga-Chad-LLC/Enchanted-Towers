package enchantedtowers.client.interactors.canvas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import enchantedtowers.client.components.canvas.CanvasSpellDecorator;
import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_logic.SpellsPatternMatchingAlgorithm;
import enchantedtowers.game_logic.HausdorffMetric;
import enchantedtowers.game_models.utils.Point;

public class CanvasDrawSpellInteractor implements CanvasInteractor {
    private final Path path = new Path();
    private final List<Point> pathPoints = new ArrayList<>();
    private final Paint brush;

    private boolean isValidPath() {
        // The condition is required for the correct metric distance calculation
        if (pathPoints.size() < 2 || (pathPoints.size() == 2 && pathPoints.get(0).equals(pathPoints.get(1)))) {
            return false;
        }
        return true;
    }

    public CanvasDrawSpellInteractor(CanvasState state) {
        brush = state.getBrush();
    }

    @Override
    public void onDraw(CanvasState state, Canvas canvas) {
        canvas.drawPath(path, brush);
    }

    @Override
    public boolean onTouchEvent(CanvasState state, float x, float y, int motionEventType) {
        return switch (motionEventType) {
            case MotionEvent.ACTION_DOWN -> onActionDownStartNewPath(state, x, y);
            case MotionEvent.ACTION_UP -> onActionUpFinishPathAndSubstitute(state, x, y);
            case MotionEvent.ACTION_MOVE -> onActionMoveContinuePath(x, y);
            default -> false;
        };
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

    private boolean onActionDownStartNewPath(CanvasState state, float x, float y) {
        path.moveTo(x, y);
        pathPoints.add(new Point(x, y));
        // update color only when started the new shape
        brush.setColor(state.getBrushColor());

        return true;
    }

    private boolean onActionUpFinishPathAndSubstitute(CanvasState state, float x, float y) {
        path.lineTo(x, y);
        pathPoints.add(new Point(x, y));

        if (isValidPath()) {
            Spell pattern = new Spell(
                    getNormalizedPoints(pathPoints),
                    getPathOffset(path)
            );

            Optional<Spell> matchedSpell = SpellsPatternMatchingAlgorithm.getMatchedTemplate(
                    SpellBook.getTemplates(),
                    pattern,
                    new HausdorffMetric()
            );

            if (matchedSpell.isPresent()) {
                CanvasSpellDecorator canvasMatchedEnchantment = new CanvasSpellDecorator(
                        brush.getColor(),
                        matchedSpell.get()
                );

                state.addItem(canvasMatchedEnchantment);
            }
        }

        path.reset();
        pathPoints.clear();

        return true;
    }

    private boolean onActionMoveContinuePath(float x, float y) {
        path.lineTo(x, y);
        pathPoints.add(new Point(x, y));
        return true;
    }
}