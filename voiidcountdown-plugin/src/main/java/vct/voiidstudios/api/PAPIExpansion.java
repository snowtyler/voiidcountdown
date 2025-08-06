package vct.voiidstudios.api;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vct.voiidstudios.managers.TimerManager;

public class PAPIExpansion extends PlaceholderExpansion {
    private final JavaPlugin plugin;

    public PAPIExpansion(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // %vct_timer_hh% - Get the hours (00) of the timer
    // %vct_timer_mm% - Get the minutes (00) of the timer
    // %vct_timer_ss% - Get the seconds (00) of the timer

    // %vct_timer_active% - Return 'true' or 'false' if the timer is active (THERE IS A TIMER).
    // %vct_timer_running% - Return 'true' or 'false' if the timer is running (NOT PAUSED).
    // %vct_timer_paused% - Return 'true' or 'false' if the timer is paused.

    @Override
    public @NotNull String getIdentifier() {
        return "vct";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(",", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("timer_hh")) {
            return getTimerValueHH();
        } else if (params.equalsIgnoreCase("timer_mm")) {
            return getTimerValueMM();
        } else if (params.equalsIgnoreCase("timer_ss")) {
            return getTimerValueSS();
        } else if (params.equalsIgnoreCase("timer_active")) {
            return isTimerActive();
        } else if (params.equalsIgnoreCase("timer_running")) {
            return isTimerRunning();
        } else if (params.equalsIgnoreCase("timer_paused")) {
            return isTimerPaused();
        }

        return null;
    }

    private String getTimerValueHH() {
        Timer timer = TimerManager.getInstance().getTimer();
        if (timer != null)
            return timer.getTimeLeftHH();
        return null;
    }

    private String getTimerValueMM() {
        Timer timer = TimerManager.getInstance().getTimer();
        if (timer != null)
            return timer.getTimeLeftMM();
        return null;
    }

    private String getTimerValueSS() {
        Timer timer = TimerManager.getInstance().getTimer();
        if (timer != null)
            return timer.getTimeLeftSS();
        return null;
    }

    private String isTimerActive() {
        return String.valueOf(TimerManager.getInstance().getTimer() != null);
    }

    private String isTimerRunning() {
        Timer timer = TimerManager.getInstance().getTimer();
        return String.valueOf(timer != null && timer.isActive());
    }

    private String isTimerPaused() {
        Timer timer = TimerManager.getInstance().getTimer();
        return String.valueOf(timer != null && !timer.isActive());
    }
}
