package com.gigachadllc.enchantedtowers.interactors.canvas;

import android.graphics.Canvas;

import com.gigachadllc.enchantedtowers.components.canvas.CanvasState;

public interface CanvasInteractor {
    void onDraw(CanvasState state, Canvas canvas);
    boolean onTouchEvent(CanvasState state, float x, float y, int motionEventType);
}
