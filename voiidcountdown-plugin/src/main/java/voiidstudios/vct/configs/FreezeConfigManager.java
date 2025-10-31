package voiidstudios.vct.configs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.configs.model.CustomConfig;

public class FreezeConfigManager {
    private static final double DEFAULT_TIME_KEEPALIVE_SECONDS = 5.0D;  // 100 ticks

    private final CustomConfig configFile;
    private final MainConfigManager mainConfigManager;

    public FreezeConfigManager(VoiidCountdownTimer plugin, MainConfigManager mainConfigManager) {
        this.mainConfigManager = mainConfigManager;
        this.configFile = new CustomConfig("freeze.yml", plugin, null, false);
        this.configFile.registerConfig();
        migrateLegacyConfigIfNeeded();
        ensureDefaults();
    }

    public void configure() {
        reloadConfig();
    }

    public void reloadConfig() {
        configFile.reloadConfig();
        ensureDefaults();
    }

    public FileConfiguration getConfig() {
        return configFile.getConfig();
    }

    public CustomConfig getConfigFile() {
        return configFile;
    }

    public void saveConfig() {
        configFile.saveConfig();
    }

    private void migrateLegacyConfigIfNeeded() {
        FileConfiguration freezeConfig = configFile.getConfig();
        if (freezeConfig.contains("Freeze.core")) {
            return;
        }
        if (mainConfigManager == null) {
            return;
        }
        FileConfiguration legacy = mainConfigManager.getConfig();
        if (legacy == null || !legacy.contains("Freeze")) {
            return;
        }

        // Core settings
        freezeConfig.set("Freeze.core.notifications.cooldown_ms", legacy.getLong("Freeze.notifications.cooldown_ms", 1500L));
        freezeConfig.set("Freeze.core.notifications.silent", legacy.getBoolean("Freeze.notifications.silent", false));
        freezeConfig.set("Freeze.core.mobs.prevent_spawn", legacy.getBoolean("Freeze.mobs.prevent_spawn", true));
        freezeConfig.set("Freeze.core.mobs.kill_on_freeze", legacy.getBoolean("Freeze.mobs.kill_on_freeze", true));
        freezeConfig.set("Freeze.core.time.lock_midnight", legacy.getBoolean("Freeze.time.lock_midnight", true));
        freezeConfig.set("Freeze.core.time.keepalive_seconds", ticksToSeconds(legacy.getInt("Freeze.time.keepalive_ticks", secondsToTicks(DEFAULT_TIME_KEEPALIVE_SECONDS))));

        // Shared title defaults (legacy stored a single set of timings)
        double legacyFadeIn = ticksToSeconds(legacy.getInt("Freeze.title.fade_in", secondsToTicks(0.5D)));
        double legacyStay = ticksToSeconds(legacy.getInt("Freeze.title.stay", secondsToTicks(3.0D)));
        double legacyFadeOut = ticksToSeconds(legacy.getInt("Freeze.title.fade_out", secondsToTicks(1.0D)));
        double legacyKeepalive = ticksToSeconds(legacy.getInt("Freeze.title.keepalive_ticks", secondsToTicks(2.0D)));

        // Success settings
        freezeConfig.set("Freeze.success.title.text", legacy.getString("Freeze.title.success.text", legacy.getString("Freeze.title.text", "&aSuccess")));
        freezeConfig.set("Freeze.success.title.subtitle", legacy.getString("Freeze.title.success.subtitle", legacy.getString("Freeze.title.subtitle", "&7Nice work")));
        freezeConfig.set("Freeze.success.title.fade_in_seconds", legacyFadeIn);
        freezeConfig.set("Freeze.success.title.stay_seconds", legacyStay);
        freezeConfig.set("Freeze.success.title.fade_out_seconds", legacyFadeOut);
        freezeConfig.set("Freeze.success.title.keepalive_seconds", legacyKeepalive);
        freezeConfig.set("Freeze.success.music.enabled", legacy.getBoolean("Freeze.music.success.enabled", false));
        String legacyBaseSound = legacy.getString("Freeze.music.sound", "minecraft:music.menu");
        freezeConfig.set("Freeze.success.music.sound", legacy.getString("Freeze.music.success.sound", legacyBaseSound));
        freezeConfig.set("Freeze.success.music.volume", legacy.getDouble("Freeze.music.success.volume", legacy.getDouble("Freeze.music.volume", 1.0D)));
        freezeConfig.set("Freeze.success.music.pitch", legacy.getDouble("Freeze.music.success.pitch", legacy.getDouble("Freeze.music.pitch", 1.0D)));
        freezeConfig.set("Freeze.success.music.loop_seconds", legacy.getDouble("Freeze.music.success.loop_seconds", legacy.getDouble("Freeze.music.loop_seconds", 20.0D)));

        // Success script (same structure as legacy)
        freezeConfig.set("Freeze.success_script.enabled", legacy.getBoolean("Freeze.success_script.enabled", false));
        freezeConfig.set("Freeze.success_script.fire_once", legacy.getBoolean("Freeze.success_script.fire_once", true));
        double commandsDelaySecs = ticksToSeconds(legacy.getInt("Freeze.success_script.commands_delay_ticks", 0));
        if (legacy.contains("Freeze.success_script.commands_delay_seconds")) {
            commandsDelaySecs = legacy.getDouble("Freeze.success_script.commands_delay_seconds", commandsDelaySecs);
        }
        freezeConfig.set("Freeze.success_script.commands_delay_seconds", commandsDelaySecs);
        freezeConfig.set("Freeze.success_script.commands", convertLegacyCommands(legacy.getList("Freeze.success_script.commands")));

        List<Map<?, ?>> legacySequence = legacy.getMapList("Freeze.success_script.sequence");
        List<Map<String, Object>> convertedSequence = new ArrayList<>();
        for (Map<?, ?> step : legacySequence) {
            Map<String, Object> entry = new HashMap<>();
            if (step.containsKey("title")) {
                entry.put("title", step.get("title"));
            }
            if (step.containsKey("subtitle")) {
                entry.put("subtitle", step.get("subtitle"));
            }
            entry.put("fade_in_seconds", extractSeconds(step, "fade_in_seconds", "fade_in", legacyFadeIn));
            entry.put("stay_seconds", extractSeconds(step, "stay_seconds", "stay", legacyStay));
            entry.put("fade_out_seconds", extractSeconds(step, "fade_out_seconds", "fade_out", legacyFadeOut));
            entry.put("delay_seconds", extractSeconds(step, "delay_seconds", "delay_ticks", 0.0D));
            convertedSequence.add(entry);
        }
        freezeConfig.set("Freeze.success_script.sequence", convertedSequence);

        ConfigurationSection legacySuccessMusic = legacy.getConfigurationSection("Freeze.success_script.music");
        if (legacySuccessMusic != null) {
            freezeConfig.set("Freeze.success_script.music.enabled", legacySuccessMusic.getBoolean("enabled", false));
            freezeConfig.set("Freeze.success_script.music.sound", legacySuccessMusic.getString("sound"));
            freezeConfig.set("Freeze.success_script.music.volume", legacySuccessMusic.getDouble("volume", 1.0D));
            freezeConfig.set("Freeze.success_script.music.pitch", legacySuccessMusic.getDouble("pitch", 1.0D));
            if (legacySuccessMusic.contains("loop_seconds")) {
                freezeConfig.set("Freeze.success_script.music.loop_seconds", legacySuccessMusic.getDouble("loop_seconds", 5.0D));
            } else {
                freezeConfig.set("Freeze.success_script.music.loop_seconds", ticksToSeconds(legacySuccessMusic.getInt("loop_ticks", secondsToTicks(5.0D))));
            }
            freezeConfig.set("Freeze.success_script.music.initial_delay_seconds", legacySuccessMusic.getDouble("initial_delay_seconds", 0.0D));
            freezeConfig.set("Freeze.success_script.music.persist_after_sequence", legacySuccessMusic.getBoolean("persist_after_sequence", true));
        }

        // Failure settings
        freezeConfig.set("Freeze.fail.title.text", legacy.getString("Freeze.title.fail.text", "&cFailed"));
        freezeConfig.set("Freeze.fail.title.subtitle", legacy.getString("Freeze.title.fail.subtitle", "&7Try again"));
        freezeConfig.set("Freeze.fail.title.fade_in_seconds", legacyFadeIn);
        freezeConfig.set("Freeze.fail.title.stay_seconds", legacyStay);
        freezeConfig.set("Freeze.fail.title.fade_out_seconds", legacyFadeOut);
        freezeConfig.set("Freeze.fail.title.keepalive_seconds", legacyKeepalive);
        freezeConfig.set("Freeze.fail.music.enabled", legacy.getBoolean("Freeze.music.fail.enabled", false));
        freezeConfig.set("Freeze.fail.music.sound", legacy.getString("Freeze.music.fail.sound", legacyBaseSound));
        freezeConfig.set("Freeze.fail.music.volume", legacy.getDouble("Freeze.music.fail.volume", legacy.getDouble("Freeze.music.volume", 1.0D)));
        freezeConfig.set("Freeze.fail.music.pitch", legacy.getDouble("Freeze.music.fail.pitch", legacy.getDouble("Freeze.music.pitch", 1.0D)));
        freezeConfig.set("Freeze.fail.music.loop_seconds", legacy.getDouble("Freeze.music.fail.loop_seconds", legacy.getDouble("Freeze.music.loop_seconds", 20.0D)));

        // Failure script placeholder defaults (legacy had none)
        freezeConfig.set("Freeze.fail_script.enabled", false);
        freezeConfig.set("Freeze.fail_script.fire_once", false);
        freezeConfig.set("Freeze.fail_script.commands_delay_seconds", 0.0D);
        freezeConfig.set("Freeze.fail_script.commands", new ArrayList<>());
        freezeConfig.set("Freeze.fail_script.sequence", new ArrayList<>());
        ConfigurationSection failScriptMusic = freezeConfig.createSection("Freeze.fail_script.music");
        failScriptMusic.set("enabled", false);
        failScriptMusic.set("sound", legacy.getString("Freeze.music.fail.sound", legacyBaseSound));
        failScriptMusic.set("volume", legacy.getDouble("Freeze.music.fail.volume", 1.0D));
        failScriptMusic.set("pitch", legacy.getDouble("Freeze.music.fail.pitch", 1.0D));
        failScriptMusic.set("initial_delay_seconds", 0.0D);
        failScriptMusic.set("loop_seconds", 5.0D);
        failScriptMusic.set("persist_after_sequence", false);

        configFile.saveConfig();

        legacy.set("Freeze", null);
        mainConfigManager.saveConfig();

        // Remove lingering legacy sections that may remain in freeze.yml
        freezeConfig.set("Freeze.default_freeze_mobs", null);
        freezeConfig.set("Freeze.title", null);
        freezeConfig.set("Freeze.blindness", null);
        freezeConfig.set("Freeze.notifications", null);
        freezeConfig.set("Freeze.mobs", null);
        freezeConfig.set("Freeze.time", null);
        freezeConfig.set("Freeze.music", null);
        configFile.saveConfig();
    }

    private void ensureDefaults() {
        FileConfiguration config = configFile.getConfig();
        boolean updated = false;

        // Clean up any lingering old layout nodes
        if (config.contains("Freeze.default_freeze_mobs")) {
            config.set("Freeze.default_freeze_mobs", null);
            updated = true;
        }
        if (config.contains("Freeze.title")) {
            config.set("Freeze.title", null);
            updated = true;
        }
        if (config.contains("Freeze.blindness")) {
            config.set("Freeze.blindness", null);
            updated = true;
        }
        if (config.contains("Freeze.success_script")) {
            config.set("Freeze.success_script", null);
            updated = true;
        }
        if (config.contains("Freeze.fail_script")) {
            config.set("Freeze.fail_script", null);
            updated = true;
        }
        if (config.contains("Freeze.notifications") && !config.contains("Freeze.core.notifications")) {
            config.set("Freeze.notifications", null);
            updated = true;
        }
        if (config.contains("Freeze.mobs") && !config.contains("Freeze.core.mobs")) {
            config.set("Freeze.mobs", null);
            updated = true;
        }
        if (config.contains("Freeze.time") && !config.contains("Freeze.core.time")) {
            config.set("Freeze.time", null);
            updated = true;
        }
        if (config.contains("Freeze.music")) {
            config.set("Freeze.music", null);
            updated = true;
        }

        // Core freeze behaviour
        if (!config.contains("Freeze.core")) {
            config.createSection("Freeze.core");
            updated = true;
        }
        if (!config.contains("Freeze.core.notifications")) {
            config.createSection("Freeze.core.notifications");
            updated = true;
        }
        if (!config.contains("Freeze.core.notifications.cooldown_ms")) {
            config.set("Freeze.core.notifications.cooldown_ms", 1500L);
            updated = true;
        }
        if (!config.contains("Freeze.core.notifications.silent")) {
            config.set("Freeze.core.notifications.silent", false);
            updated = true;
        }
        if (!config.contains("Freeze.core.notifications.mute_chat")) {
            config.set("Freeze.core.notifications.mute_chat", false);
            updated = true;
        }
        if (!config.contains("Freeze.core.mobs")) {
            config.createSection("Freeze.core.mobs");
            updated = true;
        }
        if (!config.contains("Freeze.core.mobs.prevent_spawn")) {
            config.set("Freeze.core.mobs.prevent_spawn", true);
            updated = true;
        }
        if (!config.contains("Freeze.core.mobs.kill_on_freeze")) {
            config.set("Freeze.core.mobs.kill_on_freeze", true);
            updated = true;
        }
        if (!config.contains("Freeze.core.time")) {
            config.createSection("Freeze.core.time");
            updated = true;
        }
        if (!config.contains("Freeze.core.time.lock_midnight")) {
            config.set("Freeze.core.time.lock_midnight", true);
            updated = true;
        }

        // Success state
        if (!config.contains("Freeze.success")) {
            config.createSection("Freeze.success");
            updated = true;
        }
        if (!config.contains("Freeze.success.initial_sound")) {
            config.createSection("Freeze.success.initial_sound");
            updated = true;
        }
        if (!config.contains("Freeze.success.initial_sound.enabled")) {
            config.set("Freeze.success.initial_sound.enabled", false);
            updated = true;
        }
        if (!config.contains("Freeze.success.initial_sound.sound")) {
            config.set("Freeze.success.initial_sound.sound", "minecraft:music.menu");
            updated = true;
        }
        if (!config.contains("Freeze.success.initial_sound.volume")) {
            config.set("Freeze.success.initial_sound.volume", 1.0D);
            updated = true;
        }
        if (!config.contains("Freeze.success.initial_sound.pitch")) {
            config.set("Freeze.success.initial_sound.pitch", 1.0D);
            updated = true;
        }
        if (!config.contains("Freeze.success.initial_sound.sound_length")) {
            config.set("Freeze.success.initial_sound.sound_length", 8.0D);
            updated = true;
        }
        if (!config.contains("Freeze.success.music")) {
            config.createSection("Freeze.success.music");
            updated = true;
        }
        if (!config.contains("Freeze.success.music.enabled")) {
            config.set("Freeze.success.music.enabled", false);
            updated = true;
        }
        if (!config.contains("Freeze.success.music.sound")) {
            config.set("Freeze.success.music.sound", "prophecy:events.audio_drone");
            updated = true;
        }
        if (!config.contains("Freeze.success.music.volume")) {
            config.set("Freeze.success.music.volume", 1.0D);
            updated = true;
        }
        if (!config.contains("Freeze.success.music.pitch")) {
            config.set("Freeze.success.music.pitch", 1.0D);
            updated = true;
        }
        if (!config.contains("Freeze.success.music.loop_seconds")) {
            config.set("Freeze.success.music.loop_seconds", 9.0D);
            updated = true;
        }
        if (!config.contains("Freeze.success.sequence")) {
            config.set("Freeze.success.sequence", new ArrayList<>());
            updated = true;
        }
        if (!config.contains("Freeze.success.post_sequence")) {
            config.createSection("Freeze.success.post_sequence");
            config.set("Freeze.success.post_sequence.title", "\\uE002");
            config.set("Freeze.success.post_sequence.subtitle", "{\"text\":\"\",\"color\":\"black\",\"bold\":true,\"shadow_color\":[0, 0, 0, 0]}");
            config.set("Freeze.success.post_sequence.fade_in_seconds", 0.0D);
            config.set("Freeze.success.post_sequence.stay_seconds", 15.0D);
            config.set("Freeze.success.post_sequence.fade_out_seconds", 0.0D);
            updated = true;
        }


        // Failure state
        if (!config.contains("Freeze.fail")) {
            config.createSection("Freeze.fail");
            updated = true;
        }
        if (!config.contains("Freeze.fail.initial_sound")) {
            config.createSection("Freeze.fail.initial_sound");
            updated = true;
        }
        if (!config.contains("Freeze.fail.initial_sound.enabled")) {
            config.set("Freeze.fail.initial_sound.enabled", false);
            updated = true;
        }
        if (!config.contains("Freeze.fail.initial_sound.sound")) {
            config.set("Freeze.fail.initial_sound.sound", "prophecy:events.audio_failure_intro");
            updated = true;
        }
        if (!config.contains("Freeze.fail.initial_sound.volume")) {
            config.set("Freeze.fail.initial_sound.volume", 1.0D);
            updated = true;
        }
        if (!config.contains("Freeze.fail.initial_sound.pitch")) {
            config.set("Freeze.fail.initial_sound.pitch", 1.0D);
            updated = true;
        }
        if (!config.contains("Freeze.fail.initial_sound.sound_length")) {
            config.set("Freeze.fail.initial_sound.sound_length", 2.0D);
            updated = true;
        }
        if (!config.contains("Freeze.fail.music")) {
            config.createSection("Freeze.fail.music");
            updated = true;
        }
        if (!config.contains("Freeze.fail.music.enabled")) {
            config.set("Freeze.fail.music.enabled", false);
            updated = true;
        }
        if (!config.contains("Freeze.fail.music.sound")) {
            config.set("Freeze.fail.music.sound", "prophecy:events.audio_failure");
            updated = true;
        }
        if (!config.contains("Freeze.fail.music.volume")) {
            config.set("Freeze.fail.music.volume", 1.0D);
            updated = true;
        }
        if (!config.contains("Freeze.fail.music.pitch")) {
            config.set("Freeze.fail.music.pitch", 1.0D);
            updated = true;
        }
        if (!config.contains("Freeze.fail.music.loop_seconds")) {
            config.set("Freeze.fail.music.loop_seconds", 20.0D);
            updated = true;
        }
        if (!config.contains("Freeze.fail.sequence")) {
            config.set("Freeze.fail.sequence", new ArrayList<>());
            updated = true;
        }
        if (!config.contains("Freeze.fail.post_sequence")) {
            config.createSection("Freeze.fail.post_sequence");
            config.set("Freeze.fail.post_sequence.title", "&cFailed");
            config.set("Freeze.fail.post_sequence.subtitle", "&7Try again");
            config.set("Freeze.fail.post_sequence.fade_in_seconds", 0.5D);
            config.set("Freeze.fail.post_sequence.stay_seconds", 3.0D);
            config.set("Freeze.fail.post_sequence.fade_out_seconds", 1.0D);
            updated = true;
        }

        if (updated) {
            configFile.saveConfig();
        }
    }

    private static int secondsToTicks(double seconds) {
        double clamped = Math.max(0.0D, seconds);
        return (int) Math.round(clamped * 20.0D);
    }

    private static double ticksToSeconds(int ticks) {
        if (ticks <= 0) {
            return 0.0D;
        }
        return ticks / 20.0D;
    }

    private static int resolveInt(Object value, int defaultTicks) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultTicks;
    }

    private static double resolveDouble(Object value, double defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private static double extractSeconds(Map<?, ?> source, String secondsKey, String legacyKey, double defaultSeconds) {
        if (source.containsKey(secondsKey)) {
            return Math.max(0.0D, resolveDouble(source.get(secondsKey), defaultSeconds));
        }
        if (source.containsKey(legacyKey)) {
            return ticksToSeconds(resolveInt(source.get(legacyKey), secondsToTicks(defaultSeconds)));
        }
        return Math.max(0.0D, defaultSeconds);
    }

    private static List<Object> convertLegacyCommands(List<?> raw) {
        if (raw == null) {
            return null;
        }
        List<Object> converted = new ArrayList<>();
        for (Object entry : raw) {
            if (entry instanceof Map<?, ?> mapEntry) {
                Map<String, Object> copy = new HashMap<>();
                if (mapEntry.containsKey("command")) {
                    copy.put("command", mapEntry.get("command"));
                } else if (mapEntry.containsKey("cmd")) {
                    copy.put("command", mapEntry.get("cmd"));
                } else if (mapEntry.containsKey("run")) {
                    copy.put("command", mapEntry.get("run"));
                }
                if (mapEntry.containsKey("delay_seconds")) {
                    copy.put("delay_seconds", resolveDouble(mapEntry.get("delay_seconds"), 0.0D));
                } else if (mapEntry.containsKey("delay_ticks")) {
                    copy.put("delay_seconds", ticksToSeconds(resolveInt(mapEntry.get("delay_ticks"), 0)));
                }
                if (!copy.isEmpty()) {
                    converted.add(copy);
                }
            } else {
                converted.add(entry);
            }
        }
        return converted;
    }
}

