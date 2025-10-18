package voiidstudios.vct.api;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.managers.TimerManager;
import voiidstudios.vct.utils.Formatter;
import voiidstudios.vct.configs.model.TimerConfig;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Timer {
    private int seconds;
    private final BossBar bossbar;
    private BukkitTask task;
    private boolean hasSound;
    private String soundFinalName;
    public float soundVolume;
    public float soundPitch;
    private String timerText;
    private int initialSeconds;
    private int refreshInterval;
    // Allow up to 999:59:59 (3-digit hours)
    private final int maxValue = 3599999;
    private final int minValue = 0;
    private final String timerId;
    private static final DateTimeFormatter INSTANT_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private Instant targetEnd;
    private boolean userPaused;
    private long pausedRemainingSeconds;

    private int clampSeconds(int value) {
        return Math.max(this.minValue, Math.min(value, this.maxValue));
    }

    private int computeRemainingSeconds(boolean updateField) {
        int result;
        if (this.userPaused) {
            result = (int) Math.max(0L, Math.min((long) this.maxValue, this.pausedRemainingSeconds));
        } else if (this.targetEnd != null) {
            long diff = Duration.between(Instant.now(), this.targetEnd).getSeconds();
            result = (int) Math.max(0L, Math.min((long) this.maxValue, diff));
        } else {
            result = this.seconds;
        }

        if (updateField) {
            this.seconds = result;
        }
        return result;
    }

    private Instant applyNewRemaining(int newRemaining) {
        int clamped = clampSeconds(newRemaining);
        Instant newTarget;
        if (this.userPaused) {
            this.pausedRemainingSeconds = clamped;
            newTarget = null;
        } else {
            newTarget = Instant.now().plusSeconds(clamped);
            this.pausedRemainingSeconds = 0L;
        }
        this.targetEnd = newTarget;
        this.seconds = clamped;
        applyProgress();
        return newTarget;
    }

    private void logTargetUpdate(String reason, int previousSeconds, int newSeconds, Instant previousTarget, Instant newTarget) {
        VoiidCountdownTimer plugin = VoiidCountdownTimer.getInstance();
        if (plugin == null) {
            return;
        }

        int drift = newSeconds - previousSeconds;
        String driftText = String.format(Locale.ROOT, "%+d", drift);
        String previousTargetText = previousTarget != null ? INSTANT_FORMATTER.format(previousTarget) : "-";
        String newTargetText = newTarget != null ? INSTANT_FORMATTER.format(newTarget) : "-";

        plugin.getLogger().info(String.format(
                Locale.ROOT,
                "Timer '%s' %s: %d -> %d seconds (drift %ss). Target %s -> %s",
                this.timerId,
                reason == null ? "update" : reason,
                previousSeconds,
                newSeconds,
                driftText,
                previousTargetText,
                newTargetText
        ));
    }

    public Timer(int seconds, String timeText, String timeSound, BarColor barcolor, BarStyle barstyle, String timerId, boolean hasSoundd, float soundVolumee, float soundPitchh) {
        int clamped = clampSeconds(seconds);
        this.seconds = clamped;
        this.initialSeconds = clamped;
        this.timerId = timerId;

        this.refreshInterval = VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getRefresh_ticks();
        this.hasSound = hasSoundd;

        this.timerText = timeText;
        this.soundFinalName = timeSound;
        this.soundVolume = soundVolumee;
        this.soundPitch = soundPitchh;

        this.bossbar = Bukkit.createBossBar("", barcolor, barstyle, new org.bukkit.boss.BarFlag[0]);
        this.targetEnd = null;
        this.userPaused = false;
        this.pausedRemainingSeconds = 0L;
    }

    public int getInitialSeconds() {
        return this.initialSeconds;
    }

    public int getRemainingSeconds() {
        return computeRemainingSeconds(true);
    }

    public String getTimertext() {
        return this.timerText;
    }

    public String getTimertextFormated() {
        return this.timerText
                .replace("%HH%", formatTimeHH(this.seconds))
                .replace("%MM%", formatTimeMM(this.seconds))
                .replace("%SS%", formatTimeSS(this.seconds))
                .replace("%H1%", getTimeLeftHHDigit1())
                .replace("%H2%", getTimeLeftHHDigit2())
                .replace("%H3%", getTimeLeftHHDigit3())
                .replace("%M1%", getTimeLeftMMDigit1())
                .replace("%M2%", getTimeLeftMMDigit2())
                .replace("%S1%", getTimeLeftSSDigit1())
                .replace("%S2%", getTimeLeftSSDigit2());
    }

    public static void playSound(Player player, String actionLine) {
        // playsound: sound;volume;pitch
        String[] sep = actionLine.split(";");
        String soundName = sep[0];
        float volume = Float.parseFloat(sep[1]);
        float pitch = Float.parseFloat(sep[2]);

        boolean success = false;

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
            success = true;
        } catch (IllegalArgumentException ignored) {
            try {
                player.playSound(player.getLocation(), soundName, volume, pitch);
                success = true;
            } catch (Exception e) { /* ignore */ }
        }

        if (!success) {
            Bukkit.getLogger().warning("[TuPlugin] No se pudo reproducir el sonido: " + soundName);
        }
    }

    private void updateBossBarTitle(String phasesText) {
        Formatter formatter = VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getFormatter();
        Object formatted = formatter.format(
                VoiidCountdownTimer.getInstance(),
                Bukkit.getConsoleSender(),
                phasesText
        );

        try {
            Class<?> componentClass = null;
            try {
                componentClass = Class.forName("net.kyori.adventure.text.Component");
            } catch (ClassNotFoundException ignored) {
                componentClass = null;
            }

            if (componentClass != null && componentClass.isInstance(formatted)) {
                try {
                    this.bossbar.getClass().getMethod("setTitle", componentClass).invoke(this.bossbar, formatted);
                    return;
                } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException e) {}

                try {
                    Class<?> legacyCls = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");

                    Object serializer;
                    try {
                        serializer = legacyCls.getMethod("legacySection").invoke(null);
                    } catch (NoSuchMethodException nsme) {
                        Class<?> builderPublicClass = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer$Builder");
                        Object builder = legacyCls.getMethod("builder").invoke(null);
                        builderPublicClass.getMethod("character", char.class).invoke(builder, '&');
                        try { builderPublicClass.getMethod("hexColors").invoke(builder); } catch (NoSuchMethodException ignored) {}
                        serializer = builderPublicClass.getMethod("build").invoke(builder);
                    }

                    String legacyTitle = (String) legacyCls.getMethod("serialize", componentClass).invoke(serializer, formatted);
                    this.bossbar.setTitle(legacyTitle);
                    return;
                } catch (Throwable t) {}
            }
        } catch (Throwable t) {}

        if (formatted instanceof String) {
            this.bossbar.setTitle(((String) formatted).replace('&', '§'));
        } else {
            this.bossbar.setTitle(phasesText.replace('&', '§'));
        }
    }

    private void startTask() {
        if (this.task != null) {
            this.task.cancel();
        }

        this.task = Bukkit.getScheduler().runTaskTimer(
                VoiidCountdownTimer.getInstance(),
                new Runnable() {
                    private int tickCounter = 0;
                    private int refreshCounter = 0;
                    private boolean changedSinceLastSound = false;

                    public void run() {
                        int previous = Timer.this.seconds;
                        int current = Timer.this.computeRemainingSeconds(true);

                        if (!Timer.this.userPaused && current != previous) {
                            changedSinceLastSound = true;
                            Bukkit.getPluginManager().callEvent(new VCTEvent(Timer.this, VCTEvent.VCTEventType.CHANGE, null));
                        }

                        tickCounter++;
                        refreshCounter++;

                        if (tickCounter >= 20) {
                            tickCounter = 0;
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                Timer.this.bossbar.addPlayer(player);
                                if (!Timer.this.userPaused && changedSinceLastSound && Timer.this.hasSound && Timer.this.soundFinalName != null) {
                                    playSound(player, soundFinalName + ";" + soundVolume + ";" + soundPitch);
                                }
                            }
                            changedSinceLastSound = false;
                        }

                        if (refreshCounter >= Timer.this.refreshInterval) {
                            refreshCounter = 0;

                            String rawText = Timer.this.getTimertextFormated();
                            String phasesText = VoiidCountdownTimer.getPhasesManager().formatPhases(rawText);

                            updateBossBarTitle(phasesText);
                            Timer.this.applyProgress();
                        }

                        if (!Timer.this.userPaused && current <= 0) {
                            if (Timer.this.task != null) {
                                Timer.this.task.cancel();
                                Timer.this.task = null;
                            }

                            Bukkit.getPluginManager().callEvent(new VCTEvent(Timer.this, VCTEvent.VCTEventType.FINISH, null));

                            Bukkit.getScheduler().runTaskLater(VoiidCountdownTimer.getInstance(), () -> {
                                if (Timer.this.bossbar != null) {
                                    Timer.this.bossbar.removeAll();
                                }
                            }, VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTicks_hide_after_ending());
                        }
                    }
                },
                1L, 1L
        );
    }

    public static void refreshTimerText() {
        Timer current = TimerManager.getInstance().getTimer();
        if (current == null) return;

        current.refreshInterval = VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getRefresh_ticks();

        if (current.timerId != null) {
            try {
                TimerConfig cfg = VoiidCountdownTimer.getConfigsManager().getTimerConfig(current.timerId);
                if (cfg != null && cfg.isEnabled()) {
                    current.timerText = cfg.getText();
                    current.hasSound = cfg.isSoundEnabled();
                    current.soundVolume = cfg.getSoundVolume();
                    current.soundPitch = cfg.getSoundPitch();
                    try { current.soundFinalName = cfg.getSound(); } catch (Exception ignored) { /* keep existing */ }
                    try { current.bossbar.setColor(cfg.getColor()); } catch (Exception ignored) {}
                    return;
                }
            } catch (Throwable t) {}
        }

        current.timerText = "%HH%:%MM%:%SS%";
        current.soundFinalName = "UI_BUTTON_CLICK";
        current.hasSound = false;
        current.soundVolume = 1.0f;
        current.soundPitch = 1.0f;

        try {
            BarColor color = BarColor.WHITE;
            current.bossbar.setColor(color);
        } catch (Exception ignored) {}
    }

    public void setBossBarColor(BarColor color) {
        this.bossbar.setColor(color);
    }

    public void setBossBarStyle(BarStyle style) {
        this.bossbar.setStyle(style);
    }

    public void setSeconds(int seconds) {
        int previous = getRemainingSeconds();
        int clamped = clampSeconds(seconds);
        if (clamped == previous) {
            return;
        }

        if (clamped > this.initialSeconds) {
            this.initialSeconds = clamped;
        }

        Instant previousTarget = this.targetEnd;
        Instant newTarget = applyNewRemaining(clamped);
        logTargetUpdate("resync", previous, clamped, previousTarget, newTarget);
    }

    private void applyProgress() {
        int denominator = this.initialSeconds <= 0 ? 1 : this.initialSeconds;
        double progress = (double) this.seconds / (double) denominator;
        progress = Math.max(0.0, Math.min(1.0, progress));
        this.bossbar.setProgress(progress);
    }

    public void overrideInitialSeconds(int initialSeconds) {
        if (initialSeconds < 1) {
            initialSeconds = 1;
        }

        if (initialSeconds < this.seconds) {
            this.initialSeconds = this.seconds;
        } else {
            this.initialSeconds = initialSeconds;
        }

        applyProgress();
    }

    private String[] splitDigits(String value) {
        if (value == null || value.length() < 2) return new String[]{"0", "0", "0"};
        if (value.length() == 2) return new String[]{String.valueOf(value.charAt(0)), String.valueOf(value.charAt(1)), "0"};
        // For 3+ digit values, return first 3 digits
        return new String[]{
            String.valueOf(value.charAt(0)),
            String.valueOf(value.charAt(1)),
            value.length() >= 3 ? String.valueOf(value.charAt(2)) : "0"
        };
    }

    private String formatTime(long time) {
        long hours = time / 3600L;
        long minutes = time % 3600L / 60L;
        long seconds = time % 60L;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String formatTimeHH(long time) {
        long hours = time / 3600L;
        return String.format("%02d", hours);
    }

    private String formatTimeMM(long time) {
        long minutes = time % 3600L / 60L;
        return String.format("%02d", minutes);
    }

    private String formatTimeSS(long time) {
        long seconds = time % 60L;
        return String.format("%02d", seconds);
    }

    public String getTimerId() {
        return timerId;
    }

    public String getInitialTime() {
        return formatTime(this.initialSeconds);
    }

    public String getTimeLeft() {
        return formatTime(this.seconds);
    }

    public String getTimeLeftHH() {
        return formatTimeHH(this.seconds);
    }

    public String getTimeLeftHHDigit1() {
        return splitDigits(formatTimeHH(this.seconds))[0];
    }

    public String getTimeLeftHHDigit2() {
        return splitDigits(formatTimeHH(this.seconds))[1];
    }

    public String getTimeLeftHHDigit3() {
        return splitDigits(formatTimeHH(this.seconds))[2];
    }

    public long getTargetEndEpochMillis() {
        if (this.userPaused || this.targetEnd == null) {
            return -1L;
        }
        return this.targetEnd.toEpochMilli();
    }

    public void restoreFromState(int remainingSeconds, boolean paused, Instant storedEnd) {
        int previousSeconds = this.seconds;
        Instant previousTarget = this.targetEnd;

        int clamped = clampSeconds(remainingSeconds);
        this.userPaused = paused;
        if (paused) {
            this.pausedRemainingSeconds = clamped;
            this.targetEnd = null;
        } else {
            this.pausedRemainingSeconds = 0L;
            this.targetEnd = storedEnd != null ? storedEnd : Instant.now().plusSeconds(clamped);
        }
        this.seconds = clamped;
        applyProgress();

        if (this.initialSeconds < this.seconds) {
            this.initialSeconds = this.seconds;
        }

        logTargetUpdate("restore", previousSeconds, clamped, previousTarget, this.targetEnd);
    }

    public String getTimeLeftMM() {
        return formatTimeMM(this.seconds);
    }

    public String getTimeLeftMMDigit1() {
        return splitDigits(formatTimeMM(this.seconds))[0];
    }

    public String getTimeLeftMMDigit2() {
        return splitDigits(formatTimeMM(this.seconds))[1];
    }

    public String getTimeLeftSS() {
        return formatTimeSS(this.seconds);
    }

    public String getTimeLeftSSDigit1() {
        return splitDigits(formatTimeSS(this.seconds))[0];
    }

    public String getTimeLeftSSDigit2() {
        return splitDigits(formatTimeSS(this.seconds))[1];
    }

    public boolean isActive() {
        return task != null;
    }

    public boolean isPaused() {
        return this.userPaused;
    }

    public void start() {
        int remaining = clampSeconds(this.seconds);
        this.seconds = remaining;
        if (remaining <= 0) {
            stop();
            return;
        }

        this.userPaused = false;
        this.pausedRemainingSeconds = 0L;
        Instant previousTarget = this.targetEnd;
        if (this.targetEnd == null) {
            this.targetEnd = Instant.now().plusSeconds(remaining);
        }
        applyProgress();
        logTargetUpdate("start", remaining, remaining, previousTarget, this.targetEnd);
        startTask();
    }

    public void stop() {
        if (this.task != null){
            Bukkit.getPluginManager().callEvent(new VCTEvent(Timer.this, VCTEvent.VCTEventType.STOP, null));
            this.task.cancel();
            this.task = null;
        }

        this.userPaused = false;
        this.pausedRemainingSeconds = 0L;
        this.targetEnd = null;

        if (this.bossbar != null)
            this.bossbar.removeAll();
    }

    public void add(int addSeconds) {
        if (addSeconds <= 0) {
            return;
        }

        int previous = getRemainingSeconds();
        long newRemaining = Math.min((long) previous + addSeconds, (long) this.maxValue);
        if ((int) newRemaining == previous) {
            return;
        }

        Instant previousTarget = this.targetEnd;
        this.initialSeconds = (int) Math.min((long) this.maxValue, (long) this.initialSeconds + addSeconds);
        Instant newTarget = applyNewRemaining((int) newRemaining);
        logTargetUpdate("add", previous, (int) newRemaining, previousTarget, newTarget);
    }

    public void set(int setSeconds) {
        int previous = getRemainingSeconds();
        int clamped = clampSeconds(setSeconds);
        if (clamped == previous) {
            return;
        }
        Instant previousTarget = this.targetEnd;
        this.initialSeconds = clamped;
        Instant newTarget = applyNewRemaining(clamped);
        logTargetUpdate("set", previous, clamped, previousTarget, newTarget);
    }

    public void take(int takeSeconds) {
        if (takeSeconds <= 0) {
            return;
        }

        int previous = getRemainingSeconds();
        long newRemaining = Math.max((long) this.minValue, (long) previous - takeSeconds);
        if ((int) newRemaining == previous) {
            return;
        }

        Instant previousTarget = this.targetEnd;
        this.initialSeconds = clampSeconds(this.initialSeconds - takeSeconds);
        if (this.initialSeconds < newRemaining) {
            this.initialSeconds = (int) newRemaining;
        }
        Instant newTarget = applyNewRemaining((int) newRemaining);
        logTargetUpdate("take", previous, (int) newRemaining, previousTarget, newTarget);
    }

    public void pause() {
        if (this.userPaused) {
            return;
        }

        Instant previousTarget = this.targetEnd;
        int current = computeRemainingSeconds(true);
        this.pausedRemainingSeconds = current;
        this.userPaused = true;
        this.targetEnd = null;
        logTargetUpdate("pause", current, current, previousTarget, null);

        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    public void resume() {
        if (!this.userPaused || this.seconds <= 0) {
            return;
        }

        int previous = (int) Math.max(0L, this.pausedRemainingSeconds);
        Instant previousTarget = this.targetEnd;
        this.userPaused = false;
        this.targetEnd = Instant.now().plusSeconds(this.seconds);
        this.pausedRemainingSeconds = 0L;
        applyProgress();
        logTargetUpdate("resume", previous, this.seconds, previousTarget, this.targetEnd);
        startTask();
    }

}