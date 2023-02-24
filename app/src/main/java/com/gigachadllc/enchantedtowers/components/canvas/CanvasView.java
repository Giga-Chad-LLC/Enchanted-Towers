package com.gigachadllc.enchantedtowers.components.canvas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;


public class CanvasView extends View {
    private enum PointType {
        PATH_START,
        PATH_CONTINUATION
    };
//    private final int MAX_ENTRIES_SAVED_COUNT = 1000;
//    private final CircularFifoQueue<Triple<Pair<Float, Float>, Integer, PointType>> pointsList = new CircularFifoQueue<>(MAX_ENTRIES_SAVED_COUNT);
    private final ArrayList<Path> pathsList = new ArrayList<>();
    private final ArrayList<Integer> colorsList = new ArrayList<>();

    private Path path = new Path();
    private int nextShapeColor = Color.BLACK;
    private Paint paintBrush = new Paint();

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
        pathsList.clear();
        colorsList.clear();
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

        for (int i = 0; i < pathsList.size(); i++) {
            paintBrush.setColor(colorsList.get(i));
            canvas.drawPath(pathsList.get(i), paintBrush);
        }

        paintBrush.setColor(currentShapeColor);
    }

    private void drawCurrentPath(Canvas canvas) {
        canvas.drawPath(path, paintBrush);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                path.reset();
                path.moveTo(x, y);

                // update color only when started the new shape
                paintBrush.setColor(nextShapeColor);

                return true;
            }
            case MotionEvent.ACTION_UP: {
                path.lineTo(x, y);

                pathsList.add(new Path(path));
                colorsList.add(paintBrush.getColor());

                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                path.lineTo(x, y);
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
