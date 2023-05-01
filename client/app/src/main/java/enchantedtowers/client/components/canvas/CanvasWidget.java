package enchantedtowers.client.components.canvas;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import enchantedtowers.client.interactors.canvas.CanvasInteractor;
import enchantedtowers.common.utils.proto.requests.ToggleAttackerRequest;


public class CanvasWidget extends View {
    private CanvasState state = new CanvasState();
    private List<CanvasInteractor> interactors = new ArrayList<>();

    public CanvasWidget(Context context) {
        super(context);
    }

    public CanvasWidget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CanvasWidget(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CanvasState getState() {
        return state;
    }

    public void setInteractors(List<CanvasInteractor> interactors) {
        this.interactors = interactors;
    }

    /**
     * This method is not part of android java API (it is a custom one).
     * Method is required to stop worker inside CanvasDrawSpellInteractor (and maybe other things in the future)
     */
    public void onExecutionInterrupt() {
        for (CanvasInteractor interactor : interactors) {
            interactor.onExecutionInterrupt();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (CanvasInteractor interactor : interactors) {
            interactor.onDraw(state, canvas);
        }
    }

    // TODO: invalidated inside interators or check for `eventHandled` boolean (see onTouchEvent, onDraw, onClearCanvas methods)
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean eventHandled = false;
        for (CanvasInteractor interactor : interactors) {
            eventHandled |= interactor.onTouchEvent(state, event.getX(), event.getY(), event.getAction());
        }
        if (eventHandled) {
            invalidate();
        }
        return eventHandled;
    }

    public void onClearCanvas() {
        boolean eventHandled = false;
        for (CanvasInteractor interactor : interactors) {
            eventHandled |= interactor.onClearCanvas(state);
        }

        if (eventHandled) {
            invalidate();
        }
    }

    public void onSubmitCanvas() {
        boolean eventHandled = false;
        for (CanvasInteractor interactor : interactors) {
            eventHandled |= interactor.onSubmitCanvas(state);
        }

        if (eventHandled) {
            invalidate();
        }
    }

    public void onToggleSpectatingAttacker(ToggleAttackerRequest.RequestType requestType) {
        boolean eventHandled = false;
        for (CanvasInteractor interactor : interactors) {
            eventHandled |= interactor.onToggleSpectatingAttacker(requestType, state);
        }

        if (eventHandled) {
            invalidate();
        }
    }

    public void setBrushColor(int newColor) {
        state.setBrushColor(newColor);
    }

    public void setBrushColor(Color newColor) {
        state.setBrushColor(newColor.toArgb());
    }
}
