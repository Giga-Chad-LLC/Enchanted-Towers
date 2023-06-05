package enchantedtowers.client.components.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;

import enchantedtowers.client.R;

public class EnableGPSSuggestionDialog extends Dialog {
    public static EnableGPSSuggestionDialog newInstance(Context context, Runnable continueCallback, Runnable cancelCallback) {
        return new EnableGPSSuggestionDialog(context, continueCallback, cancelCallback);
    }

    private final Runnable positiveCallback;
    private final Runnable negativeCallback;

    private EnableGPSSuggestionDialog(Context context, Runnable positiveCallback, Runnable negativeCallback) {
        super(context);
        this.positiveCallback = positiveCallback;
        this.negativeCallback = negativeCallback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enable_gps_suggestion_dialog);

        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button positiveButton = findViewById(R.id.gps_positive_button);
        Button negativeButton = findViewById(R.id.gps_negative_button);

        positiveButton.setOnClickListener(view -> {
            dismiss();
            positiveCallback.run();
        });
        negativeButton.setOnClickListener(view -> {
            dismiss();
            negativeCallback.run();
        });
    }
}
