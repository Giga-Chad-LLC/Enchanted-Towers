package enchantedtowers.client.components.canvas;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import enchantedtowers.client.R;
import enchantedtowers.client.components.dialogs.SpellbookDialogFragment;
import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.client.interactors.canvas.CanvasInteractor;
import enchantedtowers.common.utils.proto.common.SpellType;

public class DrawableCanvasFragment extends CanvasFragment {
    private static class ImageData {
        private final ImageView icon;
        private final int defaultStateResourceId;
        private final int selectedStateResourceId;

        ImageData(ImageView icon, int defaultStateResourceId, int selectedStateResourceId) {
            this.icon = icon;
            this.defaultStateResourceId = defaultStateResourceId;
            this.selectedStateResourceId = selectedStateResourceId;
        }
    }
    private final SpellbookDialogFragment spellbookDialog = SpellbookDialogFragment.newInstance();
    private final List<ImageData> elementIconsData = new ArrayList<>();
    private int currentCanvasBrushColor = 0;
    private final List<SpellType> spellTypes = ClientUtils.getSpellTypesList();

    private void setColor(ImageData data, int newColor) {
        currentCanvasBrushColor = newColor;
        if (currentCanvasBrushColor >= spellTypes.size()) {
            currentCanvasBrushColor = 0;
        }

        if (canvasWidget != null) {
            canvasWidget.setSpellType(spellTypes.get(currentCanvasBrushColor));
        }

        // defaulting icons for all images
        for (ImageData other : elementIconsData) {
            other.icon.setImageResource(other.defaultStateResourceId);
        }
        // setting selected
        data.icon.setImageResource(data.selectedStateResourceId);
    }

    private void clearCanvas() {
        if (canvasWidget != null) {
            canvasWidget.onClearCanvas();
        }
    }

    private void submitCanvas() {
        if (canvasWidget != null) {
            canvasWidget.onSubmitCanvas();
        }
    }

    protected SpellType getSelectedSpellType() {
        return spellTypes.get(currentCanvasBrushColor);
    }


    /**
     * <p>Method should be called only by those canvas fragments that support canvas drawing and have elements icons panel, clear & submit buttons.</p>
     * <p>{@link DrawableCanvasFragment#canvasWidget} must be set by the descendant classes.</p>
     */
    protected void initDrawingFunctionality(View rootView, List<CanvasInteractor> interactors) {
        if (canvasWidget == null) {
            throw new RuntimeException("CanvasWidget must be set before making the method call, got null");
        }
        canvasWidget.setInteractors(interactors);
        canvasWidget.setSpellType(getSelectedSpellType());

        Button openSpellbookButton = rootView.findViewById(R.id.open_spellbook_button);
        Button clearCanvasButton = rootView.findViewById(R.id.clear_canvas_button);
        Button submitCanvasButton = rootView.findViewById(R.id.submit_enchantment_button);

        openSpellbookButton.setOnClickListener(v -> spellbookDialog.show(getParentFragmentManager(), spellbookDialog.getTag()));

        clearCanvasButton.setOnClickListener(v -> clearCanvas());
        submitCanvasButton.setOnClickListener(v -> submitCanvas());

        // setting up element icons click listeners
        initElementIconsFunctionality(rootView);
    }

    private void initElementIconsFunctionality(View rootView) {
        ImageView fireElementIcon = rootView.findViewById(R.id.fire_element_panel_icon);
        ImageView windElementIcon = rootView.findViewById(R.id.wind_element_panel_icon);
        ImageView earthElementIcon = rootView.findViewById(R.id.earth_element_panel_icon);
        ImageView waterElementIcon = rootView.findViewById(R.id.water_element_panel_icon);

        ImageData fireImageData = new ImageData(fireElementIcon, R.drawable.fire_icon, R.drawable.fire_stroke_icon);
        ImageData windImageData = new ImageData(windElementIcon, R.drawable.wind_icon, R.drawable.wind_stroke_icon);
        ImageData earthImageData = new ImageData(earthElementIcon, R.drawable.earth_icon, R.drawable.earth_stroke_icon);
        ImageData waterImageData = new ImageData(waterElementIcon, R.drawable.water_icon, R.drawable.water_stroke_icon);

        elementIconsData.addAll(List.of(fireImageData, windImageData, earthImageData, waterImageData));

        // setting default color
        setColor(fireImageData, ClientUtils.getSpellTypesList().indexOf(SpellType.FIRE_SPELL));

        fireElementIcon.setOnClickListener(v -> setColor(fireImageData, ClientUtils.getSpellTypesList().indexOf(SpellType.FIRE_SPELL)));
        windElementIcon.setOnClickListener(v -> setColor(windImageData, ClientUtils.getSpellTypesList().indexOf(SpellType.WIND_SPELL)));
        earthElementIcon.setOnClickListener(v -> setColor(earthImageData, ClientUtils.getSpellTypesList().indexOf(SpellType.EARTH_SPELL)));
        waterElementIcon.setOnClickListener(v -> setColor(waterImageData, ClientUtils.getSpellTypesList().indexOf(SpellType.WATER_SPELL)));
    }

    @Override
    public void onDestroy() {
        var dialog = spellbookDialog.getDialog();
        if (dialog != null && dialog.isShowing()) {
            spellbookDialog.dismiss();
        }

        super.onDestroy();
    }
}
