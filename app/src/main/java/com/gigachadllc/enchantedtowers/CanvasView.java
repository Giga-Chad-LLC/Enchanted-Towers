package com.gigachadllc.enchantedtowers;

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

import java.util.ArrayList;
import java.util.TimerTask;
import java.util.Timer;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.tuple.Triple;


public class CanvasView extends View {
    private enum PointType {
        PATH_START,
        PATH_CONTINUATION
    };
    private final int MAX_POINTS_SAVED_COUNT = 100;
    private CircularFifoQueue<Triple<Float, Float, PointType>> pointsList = new CircularFifoQueue<>(MAX_POINTS_SAVED_COUNT);
    private Timer timer;

    public Path path = new Path();
    public Paint paintBrush = new Paint();

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

    private void init(Context context) {
        paintBrush.setAntiAlias(true);
        paintBrush.setColor(Color.BLACK);
        paintBrush.setStyle(Paint.Style.STROKE);
        paintBrush.setStrokeCap(Paint.Cap.ROUND);
        paintBrush.setStrokeJoin(Paint.Join.ROUND);
        paintBrush.setStrokeWidth(10f);
    }

    private void drawPaths(Canvas canvas) {
        Triple<Float, Float, PointType> point;

        while (!pointsList.isEmpty()) {
            point = pointsList.remove();

            float x = point.getLeft();
            float y = point.getMiddle();
            PointType type = point.getRight();

            switch (type) {
                case PATH_START: {
                    path.moveTo(x, y);
                    break;
                }
                case PATH_CONTINUATION: {
                    path.lineTo(x, y);
                    break;
                }
            }
        }

        canvas.drawPath(path, paintBrush);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                pointsList.add(Triple.of(x, y, PointType.PATH_START));
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_MOVE: {
                pointsList.add(Triple.of(x, y, PointType.PATH_CONTINUATION));
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
        drawPaths(canvas);
    }
}
