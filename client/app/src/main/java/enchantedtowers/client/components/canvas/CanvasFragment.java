package enchantedtowers.client.components.canvas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import enchantedtowers.client.R;
import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.client.interactors.canvas.CanvasDrawStateInteractor;
import enchantedtowers.client.interactors.canvas.CanvasInteractor;
import enchantedtowers.client.interactors.canvas.CanvasProtectionInteractor;
import enchantedtowers.common.utils.proto.common.SpellType;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CanvasFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CanvasFragment extends Fragment {
    protected CanvasWidget canvasWidget = null;

    public CanvasFragment() {
        super(R.layout.fragment_canvas_attack);
    }

    public static CanvasFragment newInstance() {
        return new CanvasFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflateFragment(R.layout.fragment_canvas_attack, inflater, container);
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
            boolean constrainedToEnd,
            int marginHorizontal,
            int marginVertical
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
            layoutParams.setMargins(0, 0, marginHorizontal, marginVertical);
        }
        else {
            layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            layoutParams.setMargins(marginHorizontal, marginVertical, 0, 0);
        }

        button.setLayoutParams(layoutParams);

        // add the Button to the layout
        constraintLayout.addView(button);
    }


    @Override
    public void onDestroy() {
        if (canvasWidget != null) {
            canvasWidget.onExecutionInterrupt();
        }
        super.onDestroy();
    }

    protected void registerOnClickActionOnView(View view, int itemId, Runnable action) {
        View trigger = view.findViewById(itemId);
        trigger.setOnClickListener(view1 -> action.run());
    }
}
