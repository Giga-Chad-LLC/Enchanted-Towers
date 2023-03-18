package enchantedtowers.client.interactors.canvas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

import enchantedtowers.client.components.canvas.CanvasEnchantment;
import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.client.components.enchantment.Enchantment;
import enchantedtowers.client.components.enchantment.EnchantmentBook;
import enchantedtowers.client.components.enchantment.EnchantmentsPatternMatchingAlgorithm;
import enchantedtowers.client.components.enchantment.HausdorffMetric;
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
                    Enchantment pattern = new Enchantment(
                            getNormalizedPoints(pathPoints),
                            getPathOffset(path)
                    );

                    Enchantment matchedEnchantment = EnchantmentsPatternMatchingAlgorithm.getMatchedTemplate(
                            EnchantmentBook.getTemplates(),
                            pattern,
                            new HausdorffMetric()
                    );

                    if (matchedEnchantment != null) {
                        CanvasEnchantment canvasMatchedEnchantment = new CanvasEnchantment(
                                matchedEnchantment.getPath(),
                                brush.getColor(),
                                matchedEnchantment
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