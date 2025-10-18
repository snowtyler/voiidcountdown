package voiidstudios.vct.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.api.Timer;
import voiidstudios.vct.configs.model.CustomConfig;
import voiidstudios.vct.utils.TimerDefaults;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TimerStateManager {
    private static final DateTimeFormatter INSTANT_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private final CustomConfig stateConfig;

    public TimerStateManager(VoiidCountdownTimer plugin) {
        this.stateConfig = new CustomConfig("timer_state.yml", plugin, null, true);
        this.stateConfig.registerConfig();
    }

    public void saveState() {
        FileConfiguration cfg = stateConfig.getConfig();

        Timer timer = TimerManager.getInstance().getTimer();

        if (timer == null) {
            cfg.set("active", false);
            cfg.set("timer_id", null);
            cfg.set("initial", null);
            cfg.set("remaining", null);
            cfg.set("paused", null);
            stateConfig.saveConfig();
            VoiidCountdownTimer.getInstance().getLogger().info("Timer state cleared (no active timer).");
            return;
        }

        Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(VoiidCountdownTimer.prefix+"&rSaving the state of timer " + timer.getTimerId()));

        int remainingSeconds = timer.getRemainingSeconds();
        long targetMillisRaw = timer.getTargetEndEpochMillis();
        Object targetPersistValue = targetMillisRaw > 0L ? targetMillisRaw : null;

        cfg.set("active", true);
        cfg.set("timer_id", timer.getTimerId());
        cfg.set("initial", timer.getInitialSeconds());
        cfg.set("remaining", remainingSeconds);
        cfg.set("paused", timer.isPaused());
        cfg.set("end_epoch_millis", targetPersistValue);
        stateConfig.saveConfig();

        String targetText = targetPersistValue != null
            ? INSTANT_FORMATTER.format(Instant.ofEpochMilli((Long) targetPersistValue))
            : "-";
        VoiidCountdownTimer.getInstance().getLogger().info(String.format(
            Locale.ROOT,
            "Saved timer '%s': remaining %d/%d seconds, paused=%s, target=%s",
            timer.getTimerId(),
            remainingSeconds,
            timer.getInitialSeconds(),
            timer.isPaused(),
            targetText
        ));
    }

    public void loadState() {
        FileConfiguration cfg = stateConfig.getConfig();

        if (!cfg.contains("active") || !cfg.getBoolean("active", false)) return;

        String savedId = cfg.getString("timer_id", null);
        int initial = cfg.getInt("initial", -1);
        int remaining = cfg.getInt("remaining", -1);
        long endEpochMillis = cfg.contains("end_epoch_millis") ? cfg.getLong("end_epoch_millis", -1L) : -1L;
        boolean paused = cfg.getBoolean("paused", false);

        if (initial <= 0 || remaining <= 0) return;

        Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(VoiidCountdownTimer.prefix+"&rLoading the state of timer " + savedId));
        TimerDefaults.TimerSettings settings = TimerDefaults.getSettings(savedId);
        String usedId = savedId;

        Timer timer = new Timer(
                initial,
                settings.text,
                settings.sound,
                settings.color,
                settings.style,
                usedId,
                settings.hasSound,
                settings.volume,
                settings.pitch
        );

        TimerManager.getInstance().removeTimer();
        Instant storedEnd = endEpochMillis > 0L ? Instant.ofEpochMilli(endEpochMillis) : null;
        timer.restoreFromState(remaining, paused, storedEnd);
        int effectiveRemaining = timer.getRemainingSeconds();

        TimerManager.getInstance().setTimer(timer);

        String storedTargetText = storedEnd != null ? INSTANT_FORMATTER.format(storedEnd) : "-";
        int drift = effectiveRemaining - remaining;
        Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(VoiidCountdownTimer.prefix+"&aLoaded the state of timer " + savedId + " &e(" + effectiveRemaining + "/" + initial + " seconds | Paused: " + paused + " | Target: " + storedTargetText + " | Drift: " + (drift >= 0 ? "+" : "") + drift + "s)"));
        VoiidCountdownTimer.getInstance().getLogger().info(String.format(
                Locale.ROOT,
                "Loaded timer '%s': remaining %d/%d seconds, paused=%s, target=%s, drift=%+ds",
                savedId,
                effectiveRemaining,
                initial,
                paused,
                storedTargetText,
                drift
        ));

        if (!paused && effectiveRemaining > 0) {
            timer.start();
        }
    }
}