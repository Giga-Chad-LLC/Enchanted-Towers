package com.gigachadllc.enchantedtowers.components.canvas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.gigachadllc.enchantedtowers.components.drawable.Enchantment;

import java.util.ArrayList;


public class CanvasView extends View {
    private final ArrayList<CanvasItem> enchantmentsList = new ArrayList<>();

    private final Path path = new Path();
    private final ArrayList<PointF> pathPoints = new ArrayList<>();
    private final Paint paintBrush = new Paint();

    private int nextShapeColor = Color.BLACK;

    public CanvasView(Context context) {
        super(context);
        init(context);
    }

    public CanvasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CanvasView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void setBrushColor(int newColor) {
        nextShapeColor = newColor;
    }

    public void setBrushColor(Color newColor) {
        nextShapeColor = newColor.toArgb();
    }

    public void clearCanvas() {
        enchantmentsList.clear();
        path.reset();
        invalidate();
    }

    private void init(Context context) {
        paintBrush.setAntiAlias(true);
        paintBrush.setColor(Color.BLACK);
        paintBrush.setStyle(Paint.Style.STROKE);
        paintBrush.setStrokeCap(Paint.Cap.ROUND);
        paintBrush.setStrokeJoin(Paint.Join.ROUND);
        paintBrush.setStrokeWidth(10f);
    }

    private void drawPreviousPaths(Canvas canvas) {
        int currentShapeColor = paintBrush.getColor();

        for (int i = 0; i < enchantmentsList.size(); i++) {
            enchantmentsList.get(i).drawOnCanvas(canvas, paintBrush);
        }

        paintBrush.setColor(currentShapeColor);
    }

    private void drawCurrentPath(Canvas canvas) {
        canvas.drawPath(path, paintBrush);
    }

    public ArrayList<PointF> getNormalizedPoints(
            ArrayList<PointF> points,
            PointF offset
    ) {
        ArrayList<PointF> translatedPoints = new ArrayList<>(points);

        offset.negate();
        // translate each point
        for (PointF p : translatedPoints) {
            p.offset(offset.x, offset.y);
        }

        return translatedPoints;
    }

    public PointF getPathOffset(Path path) {
        // calculate bounding box for the path
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);

        return new PointF(bounds.left, bounds.top);
    }



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                path.reset();
                pathPoints.clear();

                path.moveTo(x, y);
                pathPoints.add(new PointF(x, y));
                // update color only when started the new shape
                paintBrush.setColor(nextShapeColor);

                return true;
            }
            case MotionEvent.ACTION_UP: {
                path.lineTo(x, y);
                pathPoints.add(new PointF(x, y));

                enchantmentsList.add(
                    new Enchantment(
                        new Path(path),
                        paintBrush.getColor(),
                        getNormalizedPoints(pathPoints, getPathOffset(path)),
                        getPathOffset(path)
                    )
                );

                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                path.lineTo(x, y);
                pathPoints.add(new PointF(x, y));
                invalidate();
                return true;
            }
            default: {
                return false;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawPreviousPaths(canvas);
        drawCurrentPath(canvas);
    }
}
