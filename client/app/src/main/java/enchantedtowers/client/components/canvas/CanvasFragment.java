package enchantedtowers.client.components.canvas;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import enchantedtowers.client.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CanvasFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CanvasFragment extends Fragment {
    protected CanvasWidget canvasWidget = null;

    public CanvasFragment() {
        super(R.layout.fragment_canvas);
    }

    public static CanvasFragment newInstance() {
        return new CanvasFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflateFragment(R.layout.fragment_canvas, inflater, container);
    }

    /**
     * Method should be called inside a {@code onCreateView} lifecycle method inside derived fragment classes.
     */
    protected View inflateFragment(int resId, LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(resId, container, false);
        //  Inflate shared layouts here
        return view;
    }

    protected void addButtonToConstraintLayout(
            ConstraintLayout constraintLayout,
            int buttonId,
            String buttonText,
            boolean constrainedToEnd
    ) {
        // create a new Button
        Button button = new Button(constraintLayout.getContext());
        button.setId(buttonId);
        button.setText(buttonText);
        // TODO: figure out how to create buttons with fixed size and certain text
        var layoutParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        if (constrainedToEnd) {
            layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        }
        else {
            layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        }

        button.setLayoutParams(layoutParams);

        // add the Button to the layout
        constraintLayout.addView(button);
    }


    @Override
    public void onDestroy() {
        canvasWidget.onExecutionInterrupt();

        super.onDestroy();
    }

    protected void registerOnClickActionOnView(View view, int itemId, Runnable action) {
        View trigger = view.findViewById(itemId);
        trigger.setOnClickListener(view1 -> action.run());
    }
}
