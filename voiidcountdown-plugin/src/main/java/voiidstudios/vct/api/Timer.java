package voiidstudios.vct.api;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.api.events.TimerChange;
import voiidstudios.vct.managers.TimerManager;
import voiidstudios.vct.utils.Formatter;

public class Timer implements Runnable {
    private int seconds;
    private final BossBar bossbar;
    private BukkitTask task;
    private static boolean hasSound;
    private Sound soundName;
    private static String timertext;
    private int initialSeconds;
    private static int refreshInterval;
    private int maxValue = 359999;
    private int minValue = 0;

    public Timer(int seconds, String timeText, String timeSound, int refreshinterval) {
        this.seconds = seconds;
        this.initialSeconds = seconds;
        refreshInterval = refreshinterval;

        String colorName = VoiidCountdownTimer.getMainConfigManager().getBossbar_default_color();
        BarColor color;

        try {
            color = BarColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            color = BarColor.WHITE;
        }

        timertext = timeText;
        this.soundName = Sound.valueOf(timeSound);
        this.bossbar = Bukkit.createBossBar("", color, BarStyle.SOLID, new org.bukkit.boss.BarFlag[0]);
    }

    public int getInitialSeconds() {
        return this.initialSeconds;
    }

    public String getTimertext() {
        return timertext;
    }

    public String getTimertextFormated() {
        return timertext.replace("%HH%", formatTimeHH(this.seconds)).replace("%MM%", formatTimeMM(this.seconds)).replace("%SS%", formatTimeSS(this.seconds));
    }

    private void updateBossBarTitle(String phasesText) {
        Formatter formatter = VoiidCountdownTimer.getMainConfigManager().getFormatter();
        Object formatted = formatter.format(
                VoiidCountdownTimer.getInstance(),
                Bukkit.getConsoleSender(),
                phasesText
        );

        // Intento seguro con reflection para Component (si está presente en runtime)
        try {
            Class<?> componentClass = null;
            try {
                componentClass = Class.forName("net.kyori.adventure.text.Component");
            } catch (ClassNotFoundException ignored) {
                componentClass = null;
            }

            // Si formatter devolvió un Component y la clase existe, intentamos setTitle(Component)
            if (componentClass != null && componentClass.isInstance(formatted)) {
                try {
                    // Intentar llamar setTitle(Component)
                    this.bossbar.getClass().getMethod("setTitle", componentClass).invoke(this.bossbar, formatted);
                    return;
                } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException e) {
                    // Puede que la implementación de BossBar no tenga setTitle(Component) — procedemos a serializar a legacy
                }

                // Intentar serializar Component -> String usando LegacyComponentSerializer si está disponible
                try {
                    Class<?> legacyCls = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");

                    Object serializer;
                    try {
                        // Preferir legacySection() cuando exista
                        serializer = legacyCls.getMethod("legacySection").invoke(null);
                    } catch (NoSuchMethodException nsme) {
                        // Si no existe legacySection(), construir con builder() y character('&')
                        Class<?> builderPublicClass = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer$Builder");
                        Object builder = legacyCls.getMethod("builder").invoke(null);
                        builderPublicClass.getMethod("character", char.class).invoke(builder, '&');
                        try { builderPublicClass.getMethod("hexColors").invoke(builder); } catch (NoSuchMethodException ignored) {}
                        serializer = builderPublicClass.getMethod("build").invoke(builder);
                    }

                    // serialize(component) -> String
                    String legacyTitle = (String) legacyCls.getMethod("serialize", componentClass).invoke(serializer, formatted);
                    this.bossbar.setTitle(legacyTitle);
                    return;
                } catch (Throwable t) {
                    // Si falla la serialización, continuamos al fallback
                }
            }
        } catch (Throwable t) {
            // No hacemos nada, seguiremos a fallback
        }

        // Si el formatter devolvió String, usarlo (convertir & -> §). Si no, usar phasesText como último recurso.
        if (formatted instanceof String) {
            this.bossbar.setTitle(((String) formatted).replace('&', '§'));
        } else {
            this.bossbar.setTitle(phasesText.replace('&', '§'));
        }
    }

    private void startTask(int seconds) {
        final int increment = -1;
        this.task = Bukkit.getScheduler().runTaskTimer(
                VoiidCountdownTimer.getInstance(),
                new Runnable() {
                    private int tickCounter = 0;
                    private int refreshCounter = 0;

                    public void run() {
                        tickCounter++;
                        refreshCounter++;

                        if (tickCounter >= 20) {
                            tickCounter = 0;
                            Timer.this.seconds += increment;

                            for (Player player : Bukkit.getOnlinePlayers()) {
                                Timer.this.bossbar.addPlayer(player);

                                if (hasSound) {
                                    player.playSound(player.getLocation(), Timer.this.soundName, 1.0F, 1.0F);
                                }
                            }

                            TimerChange timerChange = new TimerChange(Timer.this);
                        }

                        if (refreshCounter >= refreshInterval) {
                            refreshCounter = 0;

                            String rawText = timertext
                                    .replace("%HH%", Timer.this.formatTimeHH(Timer.this.seconds))
                                    .replace("%MM%", Timer.this.formatTimeMM(Timer.this.seconds))
                                    .replace("%SS%", Timer.this.formatTimeSS(Timer.this.seconds));

                            String phasesText = VoiidCountdownTimer.getPhasesManager().formatPhases(rawText);

                            updateBossBarTitle(phasesText);

                            double progress = (double) Timer.this.seconds / (double) Timer.this.initialSeconds;
                            progress = Math.max(0.0, Math.min(1.0, progress));
                            Timer.this.bossbar.setProgress(progress);
                        }

                        if (Timer.this.seconds <= 0) {
                            if (Timer.this.task != null)
                                Timer.this.task.cancel();
                            Bukkit.getScheduler().runTaskLater(VoiidCountdownTimer.getInstance(), () -> {
                                if (Timer.this.bossbar != null)
                                    Timer.this.bossbar.removeAll();
                            }, VoiidCountdownTimer.getMainConfigManager().getTicks_hide_after_ending());
                        }
                    }
                },
                1L, 1L
        );
    }

    public static void refreshTimerText() {
        timertext = VoiidCountdownTimer.getMainConfigManager().getTimer_bossbar_text();
        refreshInterval = VoiidCountdownTimer.getMainConfigManager().getRefresh_ticks();
        hasSound = VoiidCountdownTimer.getMainConfigManager().isTimer_sound_enabled();
    }

    public void setBossBarColor(BarColor color) {
        this.bossbar.setColor(color);
    }

    private String formatTime(long time) {
        long hours = time / 3600L;
        long minutes = time % 3600L / 60L;
        long seconds = time % 60L;
        return String.format("%02d:%02d:%02d", new Object[] {hours, minutes, seconds});
    }

    private String formatTimeHH(long time) {
        long hours = time / 3600L;
        return String.format("%02d", new Object[] {hours});
    }

    private String formatTimeMM(long time) {
        long minutes = time % 3600L / 60L;
        return String.format("%02d", new Object[] {minutes});
    }

    private String formatTimeSS(long time) {
        long seconds = time % 60L;
        return String.format("%02d", new Object[] {seconds});
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

    public String getTimeLeftMM() {
        return formatTimeMM(this.seconds);
    }

    public String getTimeLeftSS() {
        return formatTimeSS(this.seconds);
    }

    public boolean isActive() {
        return task != null;
    }

    public void start() {
        startTask(this.seconds);
    }

    public void stop() {
        if (this.task != null)
            this.task.cancel();
        if (this.bossbar != null)
            this.bossbar.removeAll();
    }

    public void add(int addSeconds) {
        if (this.seconds + addSeconds > this.maxValue) {
            this.seconds = this.maxValue;
            this.initialSeconds = this.maxValue;
        } else {
            this.seconds += addSeconds;
            this.initialSeconds += addSeconds;
        }
    }

    public void set(int setSeconds) {
        if (setSeconds > this.maxValue) {
            this.seconds = this.maxValue;
            this.initialSeconds = this.maxValue;
        } else if (setSeconds < this.minValue) {
            this.seconds = this.minValue;
            this.initialSeconds = this.minValue;
        } else {
            this.seconds = setSeconds;
            this.initialSeconds = setSeconds;
        }
    }

    public void take(int takeSeconds) {
        if (this.seconds - takeSeconds >= this.minValue) {
            this.seconds -= takeSeconds;
            this.initialSeconds -= takeSeconds;
        }
    }

    public void pause() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    public void resume() {
        if (this.task == null)
            startTask(this.seconds);
    }

    public void run() {
        this.seconds--;
        for (Player player : Bukkit.getOnlinePlayers())
            this.bossbar.addPlayer(player);
        if (this.seconds == 0) {
            Bukkit.getConsoleSender().sendMessage("timer over!");
            if (this.task != null)
                Bukkit.getScheduler().cancelTask(this.task.getTaskId());
            this.bossbar.removeAll();
            TimerManager.getInstance().removeTimer();
        }
    }
}
