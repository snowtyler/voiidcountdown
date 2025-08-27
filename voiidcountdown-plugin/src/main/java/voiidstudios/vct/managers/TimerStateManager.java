package voiidstudios.vct.managers;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.configuration.file.FileConfiguration;

import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.api.Timer;
import voiidstudios.vct.configs.model.CustomConfig;
import voiidstudios.vct.configs.model.TimerConfig;

public class TimerStateManager {
    private final VoiidCountdownTimer plugin;
    private final CustomConfig stateConfig;

    public TimerStateManager(VoiidCountdownTimer plugin) {
        this.plugin = plugin;
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
            return;
        }

        Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(VoiidCountdownTimer.prefix+"&rSaving the state of timer " + timer.getTimerId()));

        cfg.set("active", true);
        cfg.set("timer_id", timer.getTimerId());
        cfg.set("initial", timer.getInitialSeconds());
        cfg.set("remaining", timer.getRemainingSeconds());
        cfg.set("paused", timer.isPaused());
        stateConfig.saveConfig();
    }

    public void loadState() {
        FileConfiguration cfg = stateConfig.getConfig();

        if (!cfg.contains("active") || !cfg.getBoolean("active", false)) return;

        String savedId = cfg.getString("timer_id", null);
        int initial = cfg.getInt("initial", -1);
        int remaining = cfg.getInt("remaining", -1);
        boolean paused = cfg.getBoolean("paused", false);

        if (initial <= 0 || remaining <= 0) return;

        TimerConfig tcfg = null;
        if (savedId != null) {
            tcfg = VoiidCountdownTimer.getConfigsManager().getTimerConfig(savedId);
        }

        Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(VoiidCountdownTimer.prefix+"&rLoading the state of timer " + savedId));

        String text;
        String sound;
        float volume;
        float pitch;
        BarColor color;
        boolean hasSound;
        String usedId = savedId;

        if (tcfg != null && tcfg.isEnabled()) {
            text = tcfg.getText();
            sound = tcfg.getSound();
            volume = tcfg.getSoundVolume();
            pitch = tcfg.getSoundPitch();
            color = tcfg.getColor();
            hasSound = tcfg.isSoundEnabled();
        } else {
            TimerConfig defaultCfg = VoiidCountdownTimer.getConfigsManager().getTimerConfig("default");
            if (defaultCfg != null && defaultCfg.isEnabled()) {
                if (usedId == null) usedId = "default";
                text = defaultCfg.getText();
                sound = defaultCfg.getSound();
                volume = defaultCfg.getSoundVolume();
                pitch = defaultCfg.getSoundPitch();
                color = defaultCfg.getColor();
                hasSound = defaultCfg.isSoundEnabled();
            } else {
                text = "%HH%:%MM%:%SS%";
                sound = "UI_BUTTON_CLICK";
                volume = 1.0f;
                pitch = 1.0f;
                color = BarColor.WHITE;
                hasSound = false;
            }
        }

        TimerManager.getInstance().removeTimer();
        Timer timer = new Timer(initial, text, sound, color, usedId, hasSound, volume, pitch);
        timer.setSeconds(remaining);
        TimerManager.getInstance().setTimer(timer);

        Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(VoiidCountdownTimer.prefix+"&aLoaded the state of timer " + savedId + " &e(" + remaining + "/" + initial + " seconds | Paused: " + paused + ")"));

        if (!paused) {
            timer.start();
        }
    }
}