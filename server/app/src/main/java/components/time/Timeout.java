package components.time;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class Timeout {
    private final Timer timer = new Timer();

    public Timeout(long delay_ms, Runnable callback) {
        Objects.requireNonNull(callback);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                callback.run();
            }
        }, delay_ms);
    }

    public void cancel() {
        timer.cancel();
    }
}
