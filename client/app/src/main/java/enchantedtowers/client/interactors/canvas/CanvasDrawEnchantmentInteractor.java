package enchantedtowers.client.interactors.canvas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

import enchantedtowers.client.components.canvas.CanvasEnchantment;
import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.client.components.enchantment.Enchantment;
import enchantedtowers.client.components.enchantment.EnchantmentBook;
import enchantedtowers.client.components.enchantment.EnchantmentsPatternMatchingAlgorithm;
import enchantedtowers.client.components.enchantment.HausdorffMetric;

import java.util.ArrayList;

public class CanvasDrawEnchantmentInteractor implements CanvasInteractor {
    private final Path path = new Path();
    private final ArrayList<PointF> pathPoints = new ArrayList<>();
    private final Paint brush;

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

                Enchantment pattern = new Enchantment(
                        getNormalizedPoints(pathPoints),
                        getPathOffset(path)
                );

                CanvasEnchantment canvasPattern = new CanvasEnchantment(
                        new Path(path),
                        brush.getColor(),
                        pattern
                );

                state.addItem(canvasPattern);

                Enchantment matchedEnchantment = EnchantmentsPatternMatchingAlgorithm.getMatchedTemplate(
                        EnchantmentBook.getInstance().templates,
                        pattern,
                        new HausdorffMetric()
                );
                CanvasEnchantment canvasMatchedEnchantment = new CanvasEnchantment(
                        matchedEnchantment.getPath(),
                        canvasPattern.getColor(),
                        matchedEnchantment
                );

                state.addItem(canvasMatchedEnchantment);

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

    private ArrayList<PointF> getNormalizedPoints(
            ArrayList<PointF> points
    ) {
        PointF offset = getPathOffset(path);
        ArrayList<PointF> translatedPoints = new ArrayList<>(points);

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