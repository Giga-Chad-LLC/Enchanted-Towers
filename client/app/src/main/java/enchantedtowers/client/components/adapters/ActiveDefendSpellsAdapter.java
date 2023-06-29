package enchantedtowers.client.components.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import enchantedtowers.client.R;
import enchantedtowers.game_models.DefendSpell;
import enchantedtowers.game_models.SpellBook;

public class ActiveDefendSpellsAdapter extends RecyclerView.Adapter<ActiveDefendSpellsAdapter.DefendSpellViewHolder> {
    private final List<ActiveDefendSpellDescription> items;

    public ActiveDefendSpellsAdapter() {
        this.items = new ArrayList<>();
    }

    public static class ActiveDefendSpellDescription {
        private int id;
        private long totalDuration;

        public ActiveDefendSpellDescription(int defendSpellId, long totalDuration) {
            this.id = defendSpellId;
            this.totalDuration = totalDuration;
        }

        public int getId() {
            return id;
        }

        public long getTotalDuration() {
            return totalDuration;
        }
    }

    public static class DefendSpellViewHolder extends RecyclerView.ViewHolder {
        private static long TIMER_UPDATES_DELAY_MS = 1000;
        TextView nameView;
        TextView timerView;
        private Timer timer;
        private long leftTime_ms;
        boolean timerStarted = false;


        public DefendSpellViewHolder(View itemView) {
            super(itemView);

            System.out.println("Created holder: holder=" + this);

            this.nameView = itemView.findViewById(R.id.defend_spell_name);
            this.timerView = itemView.findViewById(R.id.defend_spell_timer);
        }

        public void startTimer(long totalDuration) {
            if (!timerStarted) {
                this.leftTime_ms = totalDuration;
                this.timerStarted = true;
                this.timer = new Timer();

                timer.scheduleAtFixedRate(new TimerTask() {
                    public void run() {
                        updateTimerView();

                        if (DefendSpellViewHolder.this.leftTime_ms == 0) {
                            cancelTimer();
                        }

                        DefendSpellViewHolder.this.leftTime_ms = Math.max(DefendSpellViewHolder.this.leftTime_ms - TIMER_UPDATES_DELAY_MS, 0);
                    }
                }, 0, TIMER_UPDATES_DELAY_MS);
            }
        }

        public void cancelTimer() {
            if (timer != null && timerStarted) {
                System.out.println("Handler:" + this + ", cancel timer");
                timer.cancel();
                timerStarted = false;
            }
        }

        private void updateTimerView() {
            long minutes = (leftTime_ms / 1000) / 60;
            long seconds = (leftTime_ms / 1000) % 60;

            String time = String.format(timerView
                    .getContext()
                    .getString(R.string.time_format_mm_ss), minutes, seconds);

            ((Activity)timerView.getContext()).runOnUiThread(() -> {
                timerView.setText(time);
            });
        }
    }

    @NonNull
    @Override
    public ActiveDefendSpellsAdapter.DefendSpellViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.active_defend_spell_item, parent, false);
        return new DefendSpellViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull DefendSpellViewHolder holder, int position) {
        var item = items.get(position);
        int defendSpellId = item.getId();

        DefendSpell defendSpell = SpellBook.getDefendSpellTemplateById(defendSpellId);
        if (defendSpell != null) {
            holder.nameView.setText(defendSpell.getName());
        }
        holder.startTimer(item.getTotalDuration());

        System.out.println("Adapter: bind holder, holder=" + holder + ", id=" + defendSpellId + ", duration=" + item.getTotalDuration());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public void onViewRecycled(@NonNull DefendSpellViewHolder holder) {
        super.onViewRecycled(holder);
        System.out.println("Adapter: recycle item: holder=" + holder);
        holder.cancelTimer();
    }

    public void addItem(int defendSpellId, long leftTime_ms) {
        System.out.println("Adapter: add item: id=" + defendSpellId + ", ms=" + leftTime_ms);

        items.add(new ActiveDefendSpellsAdapter.ActiveDefendSpellDescription(
                defendSpellId, leftTime_ms
        ));

        notifyItemInserted(items.size() - 1);
    }

    public void removeItem(int defendSpellId) {
        int position = 0;

        for (; position < items.size(); ++position) {
            var item = items.get(position);
            if (item.getId() == defendSpellId) {
                break;
            }
        }

        if (position < items.size()) {
            System.out.println("Adapter: remove item: id=" + defendSpellId + ", pos=" + position);

            items.remove(position);
            notifyItemRemoved(position);
        }
    }
}
