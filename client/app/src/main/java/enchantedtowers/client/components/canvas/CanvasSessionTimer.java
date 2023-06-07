package enchantedtowers.client.components.canvas;

import android.app.Activity;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import enchantedtowers.client.R;

public class CanvasSessionTimer {
    private static final Logger logger = Logger.getLogger(CanvasSessionTimer.class.getName());
    private final static long TIMER_UPDATES_DELAY_MS = 1000;

    private final Activity activity;
    private final TextView timerView;
    private long leftTime_ms;
    private final Timer timer;

    public CanvasSessionTimer(Activity activity, TextView timerView, long timeout_ms) {
        this.activity = activity;
        this.timerView = timerView;
        this.leftTime_ms = timeout_ms;
        this.timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                updateTimerView();
                CanvasSessionTimer.this.leftTime_ms = Math.max(CanvasSessionTimer.this.leftTime_ms - TIMER_UPDATES_DELAY_MS, 0);

                if (CanvasSessionTimer.this.leftTime_ms == 0) {
                    CanvasSessionTimer.this.cancel();
                }
            }
        }, 0, TIMER_UPDATES_DELAY_MS);

        logger.info("Timer " + timer + " started for " + timeout_ms + "ms with rate of " + TIMER_UPDATES_DELAY_MS + "ms");
    }

    public void cancel() {
        logger.info("Canceling timer " + timer + "...");
        timer.cancel();
    }

    private void updateTimerView() {
        long minutes = (leftTime_ms / 1000) / 60;
        long seconds = (leftTime_ms / 1000) % 60;

        String time = String.format(timerView
                .getContext()
                .getString(R.string.time_format_mm_ss), minutes, seconds);

        // update of views may only be done in UI thread
        activity.runOnUiThread(() -> timerView.setText(time));
    }
}
