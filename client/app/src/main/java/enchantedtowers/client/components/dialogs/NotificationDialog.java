package enchantedtowers.client.components.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Optional;

import enchantedtowers.client.R;

public class NotificationDialog extends Dialog {
    public static NotificationDialog newInstance(@NonNull Context context, String title, String description, String buttonMessage) {
        return newInstance(context, title, description, buttonMessage, null);
    }

    public static NotificationDialog newInstance(@NonNull Context context, String title, String description, String buttonMessage, Runnable onButtonClickCallback) {
        return new NotificationDialog(context, title, description, buttonMessage, onButtonClickCallback);
    }

    // fields
    private final Optional<Runnable> onButtonClickCallback;
    private final String title;
    private final String description;
    private final String buttonMessage;

    private NotificationDialog(Context context, String title, String description, String buttonMessage, Runnable onButtonClickCallback) {
        super(context);

        this.title = (title != null) ? title : "Dialog title";
        this.description = (description != null) ? description : "Dialog description";
        this.buttonMessage = (buttonMessage != null) ? buttonMessage : "Dismiss";
        this.onButtonClickCallback = (onButtonClickCallback != null) ? Optional.of(onButtonClickCallback) : Optional.empty();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_dialog);

        TextView titleView = findViewById(R.id.notification_title);
        TextView descriptionView = findViewById(R.id.notification_description);
        Button button = findViewById(R.id.notification_button);

        titleView.setText(title);
        descriptionView.setText(description);
        // setting button props
        button.setText(buttonMessage);
        button.setOnClickListener(view -> {
            onButtonClickCallback.ifPresent(Runnable::run);
            dismiss();
        });
    }
}
