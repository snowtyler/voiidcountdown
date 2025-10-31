package voiidstudios.vct.api;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.configs.model.TimerConfig;
import voiidstudios.vct.managers.MessagesManager;
import voiidstudios.vct.managers.TimerManager;
import voiidstudios.vct.utils.TimerDefaults;

public class VCTActions {
    /**
     * Creates an indestructible End Crystal at the given world and coordinates and places an INTERACTION
     * entity at the same spot with the scoreboard tag "shrine_interact". The interaction entity will have
     * a width of 2 and height of 3 so players can click it easily. If the INTERACTION entity type is not
     * available on the running server, only the End Crystal will be created.
     *
     * Inputs:
     * - worldName: Exact name of the target world
     * - x, y, z: Coordinates to spawn at (centered at the given location)
     *
     * Returns: The spawned EnderCrystal if successful, otherwise null.
     */
    public static org.bukkit.entity.EnderCrystal createIndestructibleEndCrystalWithInteraction(String worldName, double x, double y, double z) {
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            VoiidCountdownTimer.getInstance().getLogger().warning("createIndestructibleEndCrystalWithInteraction: world not found: " + worldName);
            return null;
        }

        // Ensure the target chunk is loaded to avoid spawn failures on some servers
        try {
            int cx = (int) Math.floor(x) >> 4;
            int cz = (int) Math.floor(z) >> 4;
            if (!world.isChunkLoaded(cx, cz)) {
                try { world.loadChunk(cx, cz, false); } catch (Throwable ignored) { world.loadChunk(cx, cz); }
            }
        } catch (Throwable ignored) {}

        org.bukkit.Location loc = new org.bukkit.Location(world, x, y, z);

        // Spawn End Crystal
        org.bukkit.entity.EnderCrystal crystal;
        try {
            crystal = world.spawn(loc, org.bukkit.entity.EnderCrystal.class, c -> {
                // Prefer API methods when available; fall back to reflection where needed
                try { c.setShowingBottom(false); } catch (Throwable ignored) {}
                // Mark as invulnerable when supported (modern servers)
                try { c.setInvulnerable(true); } catch (Throwable ignored) {
                    // Fallback via reflection for older APIs
                    try {
                        java.lang.reflect.Method m = c.getClass().getMethod("setInvulnerable", boolean.class);
                        m.invoke(c, true);
                    } catch (Throwable ignored2) {}
                }
                // Attempt to mark as persistent (method may not exist on this type; use reflection)
                try {
                    java.lang.reflect.Method m = c.getClass().getMethod("setRemoveWhenFarAway", boolean.class);
                    m.invoke(c, false);
                } catch (Throwable ignored) {}
                try { c.addScoreboardTag("shrine_crystal"); } catch (Throwable ignored) {}
            });
        } catch (Throwable t) {
            VoiidCountdownTimer.getInstance().getLogger().warning("Failed spawning End Crystal: " + t.getMessage());
            return null;
        }

        // Attempt to spawn the INTERACTION entity (available on modern Paper/Spigot)
        try {
            org.bukkit.entity.Entity interaction = null;
            org.bukkit.entity.EntityType type = null;
            try {
                type = org.bukkit.entity.EntityType.valueOf("INTERACTION");
            } catch (IllegalArgumentException iae) {
                type = null; // Not supported on this server
            }

            if (type != null) {
                interaction = world.spawnEntity(loc, type);
                if (interaction != null) {
                    // Tag it for InteractionActionManager rules
                    try { interaction.addScoreboardTag("shrine_interact"); } catch (Throwable ignored) {}

                    // Make it invulnerable if API supports it
                    try { interaction.setInvulnerable(true); } catch (Throwable ignored) {
                        try {
                            java.lang.reflect.Method m = interaction.getClass().getMethod("setInvulnerable", boolean.class);
                            m.invoke(interaction, true);
                        } catch (Throwable ignored2) {}
                    }

                    // Configure size: width=3, height=3 (method names vary across API variants)
                    final float width = 3.0f;
                    final float height = 3.0f;
                    boolean sized = false;
                    try {
                        java.lang.reflect.Method mw = interaction.getClass().getMethod("setInteractionWidth", float.class);
                        java.lang.reflect.Method mh = interaction.getClass().getMethod("setInteractionHeight", float.class);
                        mw.invoke(interaction, width);
                        mh.invoke(interaction, height);
                        sized = true;
                    } catch (Throwable ignored) {}
                    if (!sized) {
                        try {
                            java.lang.reflect.Method mw = interaction.getClass().getMethod("setWidth", float.class);
                            java.lang.reflect.Method mh = interaction.getClass().getMethod("setHeight", float.class);
                            mw.invoke(interaction, width);
                            mh.invoke(interaction, height);
                            sized = true;
                        } catch (Throwable ignored) {}
                    }

                    // Attempt to disable gravity/visibility if present (purely cosmetic/UX)
                    try { interaction.setGravity(false); } catch (Throwable ignored) {}
                    try { interaction.setSilent(true); } catch (Throwable ignored) {}
                }
            } else {
                VoiidCountdownTimer.getInstance().getLogger().info("INTERACTION entity type not available on this server; spawned only the End Crystal.");
            }
        } catch (Throwable t) {
            VoiidCountdownTimer.getInstance().getLogger().warning("Failed spawning INTERACTION entity: " + t.getMessage());
        }

        return crystal;
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

    public static boolean modifyTimer(String action, String value, @Nullable CommandSender sender) {
        MessagesManager msgManager = VoiidCountdownTimer.getMessagesManager();

        Timer timer = TimerManager.getInstance().getTimer();
        if (timer == null || action == null || action.isEmpty() || value.isEmpty()) return false;

        TimerConfig timerCfg = VoiidCountdownTimer.getConfigsManager().getTimerConfig(timer.getTimerId());
        if (timerCfg == null) {
            if(sender != null) {
                msgManager.sendConfigMessage(sender, "Messages.timerConfigNotFound", true, null);
            }
            return false;
        }

        switch (action.toLowerCase()) {
            case "add":
                int addSeconds = helper_parseTimeToSeconds(value);
                if (addSeconds <= 0) return false;
                timer.add(addSeconds);
                break;
            case "set":
                int setSeconds = helper_parseTimeToSeconds(value);
                if (setSeconds <= 0) return false;
                timer.set(setSeconds);
                break;
            case "take":
                int takeSeconds = helper_parseTimeToSeconds(value);
                if (takeSeconds <= 0) return false;
                timer.take(takeSeconds);
                break;
            case "bossbar_color":
                try {
                    BarColor color = BarColor.valueOf(value.toUpperCase(java.util.Locale.ROOT));
                    timer.setBossBarColor(color);
                    timerCfg.setColor(color);
                    VoiidCountdownTimer.getConfigsManager().saveTimerConfig(timerCfg);
                    Timer.refreshTimerText();
                } catch (IllegalArgumentException e) {
                    return false;
                }
                break;
            case "bossbar_style":
                try {
                    BarStyle style = BarStyle.valueOf(value.toUpperCase(java.util.Locale.ROOT));
                    timer.setBossBarStyle(style);
                    timerCfg.setStyle(style);
                    VoiidCountdownTimer.getConfigsManager().saveTimerConfig(timerCfg);
                    Timer.refreshTimerText();
                } catch (IllegalArgumentException e) {
                    return false;
                }
                break;
            case "sound":
                timerCfg.setSound(value);
                VoiidCountdownTimer.getConfigsManager().saveTimerConfig(timerCfg);
                Timer.refreshTimerText();
                break;
            case "sound_enable":
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) return false;
                boolean enabled = Boolean.parseBoolean(value);
                timerCfg.setSoundEnabled(enabled);
                VoiidCountdownTimer.getConfigsManager().saveTimerConfig(timerCfg);
                Timer.refreshTimerText();
                break;
            case "sound_volume":
                float vol;
                try {
                    vol = Float.parseFloat(value);
                } catch (NumberFormatException e) {
                    return false;
                }
                if (vol < 0.1f || vol > 2.0f) return false;
                timerCfg.setSoundVolume(vol);
                VoiidCountdownTimer.getConfigsManager().saveTimerConfig(timerCfg);
                timer.soundVolume = vol;
                break;
            case "sound_pitch":
                float pitch;
                try {
                    pitch = Float.parseFloat(value);
                } catch (NumberFormatException e) {
                    return false;
                }
                if (pitch < 0.1f || pitch > 2.0f) return false;
                timerCfg.setSoundPitch(pitch);
                VoiidCountdownTimer.getConfigsManager().saveTimerConfig(timerCfg);
                timer.soundPitch = pitch;
                break;
            case "text":
                timerCfg.setText(value);
                VoiidCountdownTimer.getConfigsManager().saveTimerConfig(timerCfg);
                Timer.refreshTimerText();
                break;
            default:
                return false;
        }

        Bukkit.getPluginManager().callEvent(new VCTEvent(timer, VCTEvent.VCTEventType.MODIFY, sender, action.toUpperCase(), value));
        return true;
    }

    public static Timer getTimer() {
        return TimerManager.getInstance().getTimer();
    }

    // Same functions — Without optionals
    public static Timer createTimer(String timeHHMMSS, String timerId) {
        return createTimer(timeHHMMSS, timerId, null);
    }

    public static boolean pauseTimer() {
        return pauseTimer(null);
    }

    public static boolean resumeTimer() {
        return resumeTimer(null);
    }

    public static void stopTimer() {
        stopTimer(null);
    }

    public static boolean modifyTimer(String action, String value) {
        return modifyTimer(action, value, null);
    }

    // Helpers
    public static int helper_parseTimeToSeconds(String hhmmss) {
        // Allow up to 3 digits for hours (supports 0-999 hours)
        if (hhmmss == null || !hhmmss.matches("\\d{1,3}:\\d{2}:\\d{2}")) return -1;
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
