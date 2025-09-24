package voiidstudios.vct.api;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.managers.TimerManager;
import voiidstudios.vct.utils.TimerDefaults;

/**
 * @deprecated VCTAPI has been renamed to VCTActions to distinguish it from other APIs and will be removed in v2.1.0. Please use {@link VCTActions} instead.
*/
@Deprecated
public class VCTAPI {
    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("VoiidCountdownTimer") != null
                && VoiidCountdownTimer.getInstance() != null
                && VoiidCountdownTimer.getInstance().isEnabled();
    }

    public static Timer createTimer(String timeHHMMSS, @Nullable String timerId, @Nullable CommandSender sender) {
        int totalSeconds = helper_parseTimeToSeconds(timeHHMMSS);
        if (totalSeconds <= 0) return null;

        String usedTimerId = (timerId == null || timerId.isEmpty()) ? "default" : timerId;
        TimerDefaults.TimerSettings settings = TimerDefaults.getSettings(usedTimerId);
        TimerManager.getInstance().removeTimer();

        Timer timer = new Timer(
                totalSeconds,
                settings.text,
                settings.sound,
                settings.color,
                settings.style,
                usedTimerId,
                settings.hasSound,
                settings.volume,
                settings.pitch
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
