package voiidstudios.vct.api;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.configs.model.TimerConfig;
import voiidstudios.vct.managers.TimerManager;

public class VCTActions {
    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("VoiidCountdownTimer") != null
                && VoiidCountdownTimer.getInstance() != null
                && VoiidCountdownTimer.getInstance().isEnabled();
    }

    public static Timer createTimer(String timeHHMMSS, @Nullable String timerId, @Nullable CommandSender sender) {
        int totalSeconds = helper_parseTimeToSeconds(timeHHMMSS);
        if (totalSeconds <= 0) return null;

        String usedTimerId = (timerId == null || timerId.isEmpty()) ? "default" : timerId;

        TimerConfig cfg = VoiidCountdownTimer.getConfigsManager().getTimerConfig(usedTimerId);
        String text;
        String sound;
        float soundVolume;
        float soundPitch;
        BarColor color;
        boolean hasSound;

        if (cfg != null && cfg.isEnabled()) {
            text = cfg.getText();
            sound = cfg.getSound();
            soundVolume = cfg.getSoundVolume();
            soundPitch = cfg.getSoundPitch();
            color = cfg.getColor();
            hasSound = cfg.isSoundEnabled();
        } else {
            text = "%HH%:%MM%:%SS%";
            sound = "UI_BUTTON_CLICK";
            soundVolume = 1.0f;
            soundPitch = 1.0f;
            color = BarColor.WHITE;
            hasSound = false;
        }

        TimerManager.getInstance().removeTimer();

        Timer timer = new Timer(
                totalSeconds,
                text,
                sound,
                color,
                usedTimerId,
                hasSound,
                soundVolume,
                soundPitch
        );

        timer.start();
        TimerManager.getInstance().setTimer(timer);

        Bukkit.getPluginManager().callEvent(new VCTEvent(timer, VCTEvent.VCTEventType.CREATE, sender));

        return timer;
    }

    public static boolean pauseTimer(@Nullable CommandSender sender) {
        Timer timer = TimerManager.getInstance().getTimer();
        if (timer == null) return false;

        timer.pause();
        Bukkit.getPluginManager().callEvent(new VCTEvent(timer, VCTEvent.VCTEventType.PAUSE, sender));
        return true;
    }

    public static boolean resumeTimer(@Nullable CommandSender sender) {
        Timer timer = TimerManager.getInstance().getTimer();
        if (timer == null) return false;

        timer.resume();
        Bukkit.getPluginManager().callEvent(new VCTEvent(timer, VCTEvent.VCTEventType.RESUME, sender));
        return true;
    }

    public static void stopTimer(@Nullable CommandSender sender) {
        TimerManager.getInstance().deleteTimer(sender);
    }

    public static boolean modifyTimer(String action, String timeHHMMSS, @Nullable CommandSender sender) {
        Timer timer = TimerManager.getInstance().getTimer();
        int seconds = helper_parseTimeToSeconds(timeHHMMSS);
        if (timer == null || seconds <= 0) return false;

        if ("add".equalsIgnoreCase(action)) {
            timer.add(seconds);
        } else if ("set".equalsIgnoreCase(action)) {
            timer.set(seconds);
        } else if ("take".equalsIgnoreCase(action)) {
            timer.take(seconds);
        }

        Bukkit.getPluginManager().callEvent(new VCTEvent(timer, VCTEvent.VCTEventType.MODIFY, sender));
        return true;
    }

    public static Timer getTimer() {
        return TimerManager.getInstance().getTimer();
    }

    // Helpers
    public static int helper_parseTimeToSeconds(String hhmmss) {
        if (hhmmss == null || !hhmmss.matches("\\d{1,2}:\\d{2}:\\d{2}")) return -1;
        String[] parts = hhmmss.split(":");
        try {
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int s = Integer.parseInt(parts[2]);
            if (m > 59 || s > 59) return -1;
            return h * 3600 + m * 60 + s;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
