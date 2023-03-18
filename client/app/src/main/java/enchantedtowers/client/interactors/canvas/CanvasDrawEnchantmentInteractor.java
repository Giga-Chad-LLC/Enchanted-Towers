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

public class CanvasDrawEnchantmentInteractor implements CanvasInteractor {
    private final Path path = new Path();
    private final List<PointF> pathPoints = new ArrayList<>();
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
                pathPoints.add(new PointF(x, y));
                // update color only when started the new shape
                brush.setColor(state.getBrushColor());

                return true;
            }
            case MotionEvent.ACTION_UP: {
                path.lineTo(x, y);
                pathPoints.add(new PointF(x, y));

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
                pathPoints.add(new PointF(x, y));
                return true;
            }
            default: {
                return false;
            }
        }
    }

    // returns new list of points that are relative to their bound-box
    private List<PointF> getNormalizedPoints(
            List<PointF> points
    ) {
        PointF offset = getPathOffset(path);
        List<PointF> translatedPoints = new ArrayList<>(points);

        offset.negate();
        // translate each point
        for (PointF p : translatedPoints) {
            p.offset(offset.x, offset.y);
        }

        return translatedPoints;
    }

    private PointF getPathOffset(Path path) {
        // calculate bounding box for the path
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);

        return new PointF(bounds.left, bounds.top);
    }
}