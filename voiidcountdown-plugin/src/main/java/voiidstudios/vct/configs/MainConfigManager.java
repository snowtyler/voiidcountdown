package voiidstudios.vct.configs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.configs.model.CustomConfig;
import voiidstudios.vct.utils.Formatter;

public class MainConfigManager {
    private CustomConfig configFile;

    private boolean update_notification;
    private int ticks_hide_after_ending;
    private String text_format;
    private int refresh_ticks;
    private boolean save_state_timers;
    private boolean custom_dark_wither_summon_enabled;
    // Test pillar configuration
    private boolean testpillar_unbreakable;
    private double testpillar_radius;
    private String testpillar_initial_material;
    private String testpillar_final_material;
    private int testpillar_initial_layers_per_tick;
    private int testpillar_replacement_delay_ticks;
    private boolean testpillar_sound_enabled;
    private String testpillar_sound_name;
    private float testpillar_sound_volume;
    private float testpillar_sound_pitch;
    private int testpillar_tick_interval_ticks;

    public MainConfigManager(VoiidCountdownTimer plugin){
        configFile = new CustomConfig("config.yml", plugin, null, false);
        configFile.registerConfig();
        checkConfigsUpdate();
    }

    public void configure(){
        FileConfiguration config = configFile.getConfig();

        update_notification = config.getBoolean("Config.update_notification");
        ticks_hide_after_ending = config.getInt("Config.ticks_hide_after_ending");
        text_format = config.getString("Config.text_format");
        refresh_ticks = config.getInt("Config.refresh_ticks");
        save_state_timers = config.getBoolean("Config.save_state_timers");
        custom_dark_wither_summon_enabled = config.getBoolean("Config.enable_custom_dark_wither_summon", false);

        // Test pillar defaults
        testpillar_unbreakable = config.getBoolean("TestPillar.unbreakable", true);
        testpillar_radius = config.getDouble("TestPillar.radius", 1.5D);
        testpillar_initial_material = config.getString("TestPillar.initial_material", "END_GATEWAY");
        testpillar_final_material = config.getString("TestPillar.final_material", "BEDROCK");
        testpillar_initial_layers_per_tick = Math.max(1, config.getInt("TestPillar.initial_layers_per_tick", 4));
        testpillar_replacement_delay_ticks = Math.max(0, config.getInt("TestPillar.replacement_delay_ticks", 40));
        testpillar_tick_interval_ticks = Math.max(1, config.getInt("TestPillar.tick_interval_ticks", 1));

        ConfigurationSection soundSection = config.getConfigurationSection("TestPillar.activation_sound");
        if (soundSection == null) {
            testpillar_sound_enabled = config.getBoolean("TestPillar.activation_sound.enabled", true);
            testpillar_sound_name = config.getString("TestPillar.activation_sound.sound", "minecraft:block.beacon.activate");
            testpillar_sound_volume = Math.max(0.0F, (float) config.getDouble("TestPillar.activation_sound.volume", 1.0D));
            testpillar_sound_pitch = (float) config.getDouble("TestPillar.activation_sound.pitch", 1.0D);
        } else {
            testpillar_sound_enabled = soundSection.getBoolean("enabled", true);
            testpillar_sound_name = soundSection.getString("sound", "minecraft:block.beacon.activate");
            testpillar_sound_volume = Math.max(0.0F, (float) soundSection.getDouble("volume", 1.0D));
            testpillar_sound_pitch = (float) soundSection.getDouble("pitch", 1.0D);
        }
    }

    public void reloadConfig(){
        configFile.reloadConfig();
        configure();
    }

    public FileConfiguration getConfig(){
        return configFile.getConfig();
    }

    public CustomConfig getConfigFile(){
        return this.configFile;
    }

    public void saveConfig(){
        configFile.saveConfig();
    }

    public void checkConfigsUpdate(){
        Path pathConfig = Paths.get(configFile.getRoute());
        try {
            String text = new String(Files.readAllBytes(pathConfig));

            if(!text.contains("update_notification:")){
                getConfig().set("Config.update_notification", true);
                saveConfig();
            }
            if(!text.contains("ticks_hide_after_ending:")){
                getConfig().set("Config.ticks_hide_after_ending", 60);
                saveConfig();
            }
            if(!text.contains("text_format:")){
                getConfig().set("Config.text_format", "LEGACY");
                saveConfig();
            }
            if(!text.contains("refresh_ticks:")){
                getConfig().set("Config.refresh_ticks", 10);
                saveConfig();
            }
            if(!text.contains("save_state_timers:")){
                getConfig().set("Config.save_state_timers", true);
                saveConfig();
            }
            if(!text.contains("enable_custom_dark_wither_summon:")){
                getConfig().set("Config.enable_custom_dark_wither_summon", false);
                saveConfig();
            }

            if(!text.contains("TestPillar:")){
                getConfig().set("TestPillar.unbreakable", true);
                getConfig().set("TestPillar.radius", 1.5D);
                getConfig().set("TestPillar.initial_material", "END_GATEWAY");
                getConfig().set("TestPillar.final_material", "BEDROCK");
                getConfig().set("TestPillar.initial_layers_per_tick", 4);
                getConfig().set("TestPillar.replacement_delay_ticks", 40);
                getConfig().set("TestPillar.tick_interval_ticks", 1);
                getConfig().set("TestPillar.activation_sound.enabled", true);
                getConfig().set("TestPillar.activation_sound.sound", "minecraft:block.beacon.activate");
                getConfig().set("TestPillar.activation_sound.volume", 1.0D);
                getConfig().set("TestPillar.activation_sound.pitch", 1.0D);
                saveConfig();
            } else {
                boolean updated = false;
                if(!text.contains("initial_material")) {
                    getConfig().set("TestPillar.initial_material", "END_GATEWAY");
                    updated = true;
                }
                if(!text.contains("final_material")) {
                    getConfig().set("TestPillar.final_material", "BEDROCK");
                    updated = true;
                }
                if(!text.contains("initial_layers_per_tick")) {
                    getConfig().set("TestPillar.initial_layers_per_tick", 4);
                    updated = true;
                }
                if(!text.contains("replacement_delay_ticks")) {
                    getConfig().set("TestPillar.replacement_delay_ticks", 40);
                    updated = true;
                }
                if(!text.contains("radius:")) {
                    getConfig().set("TestPillar.radius", 1.5D);
                    updated = true;
                }
                if(!text.contains("unbreakable:")) {
                    getConfig().set("TestPillar.unbreakable", true);
                    updated = true;
                }
                if(!text.contains("tick_interval_ticks")) {
                    getConfig().set("TestPillar.tick_interval_ticks", 1);
                    updated = true;
                }
                ConfigurationSection activationSoundSection = getConfig().getConfigurationSection("TestPillar.activation_sound");
                if (activationSoundSection == null) {
                    getConfig().set("TestPillar.activation_sound.enabled", true);
                    getConfig().set("TestPillar.activation_sound.sound", "minecraft:block.beacon.activate");
                    getConfig().set("TestPillar.activation_sound.volume", 1.0D);
                    getConfig().set("TestPillar.activation_sound.pitch", 1.0D);
                    updated = true;
                } else {
                    if (!activationSoundSection.contains("enabled")) {
                        getConfig().set("TestPillar.activation_sound.enabled", true);
                        updated = true;
                    }
                    if (!activationSoundSection.contains("sound") || activationSoundSection.getString("sound") == null) {
                        getConfig().set("TestPillar.activation_sound.sound", "minecraft:block.beacon.activate");
                        updated = true;
                    }
                    if (!activationSoundSection.contains("volume")) {
                        getConfig().set("TestPillar.activation_sound.volume", 1.0D);
                        updated = true;
                    }
                    if (!activationSoundSection.contains("pitch")) {
                        getConfig().set("TestPillar.activation_sound.pitch", 1.0D);
                        updated = true;
                    }
                }
                if(updated) {
                    saveConfig();
                }
            }

            // Ensure Freeze defaults exist
            if(!text.contains("Freeze:")){
                getConfig().set("Freeze.default_freeze_mobs", false);
                getConfig().set("Freeze.title.text", "&cServer Frozen");
                getConfig().set("Freeze.title.subtitle", "&7Please wait");
                getConfig().set("Freeze.title.fade_in", 10);
                getConfig().set("Freeze.title.stay", 60);
                getConfig().set("Freeze.title.fade_out", 20);
                getConfig().set("Freeze.blindness.enabled", true);
                getConfig().set("Freeze.blindness.amplifier", 0);
                getConfig().set("Freeze.blindness.ambient", false);
                getConfig().set("Freeze.blindness.particles", false);
                getConfig().set("Freeze.notifications.cooldown_ms", 1500L);
                getConfig().set("Freeze.notifications.silent", false);
                getConfig().set("Freeze.title.keepalive_ticks", 40);
                getConfig().set("Freeze.mobs.prevent_spawn", true);
                getConfig().set("Freeze.mobs.kill_on_freeze", true);
                getConfig().set("Freeze.time.lock_midnight", true);
                getConfig().set("Freeze.time.keepalive_ticks", 100);
                getConfig().set("Freeze.music.enabled", false);
                getConfig().set("Freeze.music.sound", "minecraft:music.menu");
                getConfig().set("Freeze.music.volume", 1.0D);
                getConfig().set("Freeze.music.pitch", 1.0D);
                getConfig().set("Freeze.music.loop_seconds", 20D);
                saveConfig();
            }
            else {
                boolean updated = false;
                // Use config.contains() for nested keys instead of raw text search to avoid false negatives
                if (!getConfig().contains("Freeze.notifications.silent")) {
                    getConfig().set("Freeze.notifications.silent", false);
                    updated = true;
                }
                if (!getConfig().contains("Freeze.music.loop_seconds")) {
                    int legacyTicks = getConfig().getInt("Freeze.music.loop_ticks", 400);
                    double secs = Math.max(1D, legacyTicks / 20D);
                    getConfig().set("Freeze.music.loop_seconds", secs);
                    updated = true;
                }
                if (updated) saveConfig();
            }

            if(!text.contains("timerSetError:")){
                getConfig().set("Messages.timerSetError", "&cUse: /vct set <HH:MM:SS>");
                getConfig().set("Messages.timerSetFormatIncorrect", "&cIncorrect format. Please use HH:MM:SS");
                getConfig().set("Messages.timerSetFormatInvalid", "&cThe format does not contain a valid number.");
                getConfig().set("Messages.timerSetFormatOutRange", "&cThe timer must be greater than 0 seconds.");
                saveConfig();
            }
            if(!text.contains("timerModifyInvalid:")){
                getConfig().set("Messages.timerModifyInvalid", "&cUse: /vct modify <modifier>");
                saveConfig();
            }
            if(!text.contains("timerModifyAddError:")){
                getConfig().set("Messages.timerModifyAddError", "&cUse: /vct modify add <HH:MM:SS>");
                getConfig().set("Messages.timerModifyAdd", "&a%HH%:%MM%:%SS% has been added to the timer.");
                saveConfig();
            }
            if(!text.contains("timerModifySetError:")){
                getConfig().set("Messages.timerModifySetError", "&cUse: /vct modify set <HH:MM:SS>");
                getConfig().set("Messages.timerModifySet", "&aThe timer was set to %HH%:%MM%:%SS%.");
                saveConfig();
            }
            if(!text.contains("timerModifyTakeError:")){
                getConfig().set("Messages.timerModifyTakeError", "&cUse: /vct modify take <HH:MM:SS>");
                getConfig().set("Messages.timerModifyTake", "&a%HH%:%MM%:%SS% has been removed from the timer.");
                saveConfig();
            }
            if(!text.contains("timerModifyBarcolorError:")){
                getConfig().set("Messages.timerModifyBarcolorError", "&cUse: /vct modify barcolor <color>. &eYou can use these colors: BLUE, GREEN, PINK, PURPLE, RED, WHITE, or YELLOW.");
                getConfig().set("Messages.timerModifyBarcolorInvalid", "&cThe color \"%COLOR%\" of the timer boss bar is invalid. Use BLUE, GREEN, PINK, PURPLE, RED, WHITE, or YELLOW.");
                getConfig().set("Messages.timerModifyBarcolor", "&aThe color of the timer %TIMER% has been changed to \"%COLOR%\".");
                saveConfig();
            }
            if(!text.contains("timerModifyBarstyleError:")){
                getConfig().set("Messages.timerModifyBarstyleError", "&cUse: /vct modify bossbar_style <style>. &eYou can use these styles: SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, or SEGMENTED_20.");
                getConfig().set("Messages.timerModifyBarstyleInvalid", "&cThe style \"%STYLE%\" of the timer boss bar is invalid. Use SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, or SEGMENTED_20.");
                getConfig().set("Messages.timerModifyBarstyle", "&aThe style of the timer %TIMER% has been changed to \"%STYLE%\".");
                saveConfig();
            }
            if(!text.contains("timerModifySoundError:")){
                getConfig().set("Messages.timerModifySoundError", "&cUse: /vct modify sound <\"sound in quotes\">");
                getConfig().set("Messages.timerModifySoundRequireQuotes", "&cThe sound needs to be enclosed in quotation marks (\") in order to change it.");
                getConfig().set("Messages.timerModifySound", "&aThe sound of the timer %TIMER% has been changed to \"%SOUND%\" &e(%TYPE%)&a.");
                saveConfig();
            }
            if(!text.contains("timerModifySoundenableError:")){
                getConfig().set("Messages.timerModifySoundenableError", "&cUse: /vct modify soundenable <true|false>");
                getConfig().set("Messages.timerModifySoundenableInvalid", "&cThe boolean is invalid. Use TRUE or FALSE.");
                getConfig().set("Messages.timerModifySoundenable", "&aThe sound enable of the timer %TIMER% has been changed to %SOUNDENABLE%.");
                saveConfig();
            }
            if(!text.contains("timerModifySoundvolumeError:")){
                getConfig().set("Messages.timerModifySoundvolumeError", "&cUse: /vct modify sound_volume <0.1 - 2.0>");
                getConfig().set("Messages.timerModifySoundvolumeInvalid", "&cThe number is invalid. Please use a number between &e0.1 &cand &e2.0.");
                getConfig().set("Messages.timerModifySoundvolumeOutRange", "&cThe volume must be between &e0.1 &cand &e2.0&c.");
                getConfig().set("Messages.timerModifySoundvolume", "&aThe sound_volume of the timer %TIMER% has been changed to &e%VOLUME%&a.");
                saveConfig();
            }
            if(!text.contains("timerModifySoundpitchError:")){
                getConfig().set("Messages.timerModifySoundpitchError", "&cUse: /vct modify sound_pitch <0.1 - 2.0>");
                getConfig().set("Messages.timerModifySoundpitchInvalid", "&cThe number is invalid. Please use a number between &e0.1 &cand &e2.0.");
                getConfig().set("Messages.timerModifySoundpitchOutRange", "&cThe pitch must be between &e0.1 &cand &e2.0&c.");
                getConfig().set("Messages.timerModifySoundpitch", "&aThe sound_pitch of the timer %TIMER% has been changed to &e%PITCH%&a.");
                saveConfig();
            }
            if(!text.contains("timerModifyTextError:")){
                getConfig().set("Messages.timerModifyTextError", "&cUse: /vct modify text <\"text in quotes\">");
                getConfig().set("Messages.timerModifyTextRequireQuotes", "&cThe text needs to be enclosed in quotation marks (\") in order to change it.");
                getConfig().set("Messages.timerModifyText", "&aThe text of the timer %TIMER% has been changed to \"&r%TEXT%&a\".");
                saveConfig();
            }
            if(!text.contains("timerStart:")){
                getConfig().set("Messages.timerStart", "&aTimer started of %HH%:%MM%:%SS%!");
                saveConfig();
            }
            if(!text.contains("timerPause:")){
                getConfig().set("Messages.timerPause", "&6Timer paused!");
                saveConfig();
            }
            if(!text.contains("timerResume:")){
                getConfig().set("Messages.timerResume", "&6Timer resumed!");
                saveConfig();
            }
            if(!text.contains("timerStop:")){
                getConfig().set("Messages.timerStop", "&6Timer stopped!");
                saveConfig();
            }
            if(!text.contains("timerDontExists:")){
                getConfig().set("Messages.timerDontExists", "&cThe timer does not exist.");
                saveConfig();
            }
            if(!text.contains("timerConfigNotFound:")){
                getConfig().set("Messages.timerConfigNotFound", "&cThe timer configuration could not be found.");
                saveConfig();
            }

            // Ensure Freeze messages exist
            if(!text.contains("freezeUsage:")){
                getConfig().set("Messages.freezeUsage", "&cUse: /vct freeze <on|off|toggle|status> [mobs|nomobs]");
                saveConfig();
            }
            if(!text.contains("freezeEnabled:")){
                getConfig().set("Messages.freezeEnabled", "&cServer freeze enabled. &7Mob freeze: &f%MOBS%");
                saveConfig();
            }
            if(!text.contains("freezeUpdated:")){
                getConfig().set("Messages.freezeUpdated", "&aServer freeze updated. &7Mob freeze: &f%MOBS%");
                saveConfig();
            }
            if(!text.contains("freezeAlreadyEnabled:")){
                getConfig().set("Messages.freezeAlreadyEnabled", "&eServer is already frozen. &7Mob freeze: &f%MOBS%");
                saveConfig();
            }
            if(!text.contains("freezeDisabled:")){
                getConfig().set("Messages.freezeDisabled", "&aServer freeze disabled.");
                saveConfig();
            }
            if(!text.contains("freezeAlreadyDisabled:")){
                getConfig().set("Messages.freezeAlreadyDisabled", "&eServer is not currently frozen.");
                saveConfig();
            }
            if(!text.contains("freezeStatus:")){
                getConfig().set("Messages.freezeStatus", "&7Freeze state: &f%STATE% &7| Mob freeze: &f%MOBS%");
                saveConfig();
            }
            if(!text.contains("freezeMobArgInvalid:")){
                getConfig().set("Messages.freezeMobArgInvalid", "&cUnknown mobs argument. Use MOBS or NOMOBS.");
                saveConfig();
            }
            if(!text.contains("freezeBlockedAction:")){
                getConfig().set("Messages.freezeBlockedAction", "&cYou cannot do that while the server is frozen.");
                saveConfig();
            }
            if(!text.contains("freezeBlockedInventory:")){
                getConfig().set("Messages.freezeBlockedInventory", "&cYou cannot access inventories while the server is frozen.");
                saveConfig();
            }
            if(!text.contains("freezeBlockedChat:")){
                getConfig().set("Messages.freezeBlockedChat", "&cYou cannot chat while the server is frozen.");
                saveConfig();
            }

            if (getConfig().contains("Timers")) {
                for (String timerKey : getConfig().getConfigurationSection("Timers").getKeys(false)) {
                    String base = "Timers." + timerKey + ".";

                    if (!getConfig().contains(base + "text")) {
                        getConfig().set(base + "text", "%HH%:%MM%:%SS%");
                    }
                    if (!getConfig().contains(base + "sound")) {
                        getConfig().set(base + "sound", "UI_BUTTON_CLICK");
                    }
                    if (!getConfig().contains(base + "sound_volume")) {
                        getConfig().set(base + "sound_volume", 1.0);
                    }
                    if (!getConfig().contains(base + "sound_pitch")) {
                        getConfig().set(base + "sound_pitch", 1.0);
                    }
                    if (!getConfig().contains(base + "bossbar_color")) {
                        getConfig().set(base + "bossbar_color", "WHITE");
                    }
                    if (!getConfig().contains(base + "bossbar_style")) {
                        getConfig().set(base + "bossbar_style", "SOLID");
                    }
                    if (!getConfig().contains(base + "enabled")) {
                        getConfig().set(base + "enabled", true);
                    }
                    if (!getConfig().contains(base + "sound_enabled")) {
                        getConfig().set(base + "sound_enabled", false);
                    }
                }
                saveConfig();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isUpdate_notification() {
        return update_notification;
    }

    public boolean isSave_state_timers() {
        return save_state_timers;
    }

    public int getTicks_hide_after_ending() {
        return ticks_hide_after_ending;
    }

    public Formatter getFormatter() {
        try {
            return Formatter.valueOf(text_format.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return Formatter.LEGACY;
        }
    }

    public int getRefresh_ticks() {
        return refresh_ticks;
    }

    // Test pillar getters
    public boolean isTestPillarUnbreakable() { return testpillar_unbreakable; }
    public double getTestPillarRadius() { return testpillar_radius; }
    public String getTestPillarInitialMaterial() { return testpillar_initial_material; }
    public String getTestPillarFinalMaterial() { return testpillar_final_material; }
    public int getTestPillarInitialLayersPerTick() { return testpillar_initial_layers_per_tick; }
    public int getTestPillarReplacementDelayTicks() { return testpillar_replacement_delay_ticks; }
    public int getTestPillarTickIntervalTicks() { return testpillar_tick_interval_ticks; }
    public boolean isTestPillarSoundEnabled() { return testpillar_sound_enabled; }
    public String getTestPillarSound() { return testpillar_sound_name; }
    public float getTestPillarSoundVolume() { return testpillar_sound_volume; }
    public float getTestPillarSoundPitch() { return testpillar_sound_pitch; }

    public boolean isCustomDarkWitherSummonEnabled() {
        return custom_dark_wither_summon_enabled;
    }
}
