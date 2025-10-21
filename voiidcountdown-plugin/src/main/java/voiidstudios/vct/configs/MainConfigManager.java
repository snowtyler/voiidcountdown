package voiidstudios.vct.configs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    private int testpillar_x;
    private int testpillar_z;
    private Integer testpillar_start_y; // nullable; if null use world minHeight
    private int testpillar_height; // 0 means full height (min->max)
    private boolean testpillar_unbreakable;
    private double testpillar_radius;
    private double testpillar_thickness;
    private int testpillar_layers_per_tick;
    private double testpillar_gateway_chance; // Default value will be set in configure method
    private String testpillar_block_type;

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
        if (config.contains("TestPillar.x")) {
            testpillar_x = config.getInt("TestPillar.x", 0);
        } else {
            testpillar_x = 0;
        }
        if (config.contains("TestPillar.z")) {
            testpillar_z = config.getInt("TestPillar.z", 0);
        } else {
            testpillar_z = 0;
        }
        if (config.contains("TestPillar.start_y")) {
            testpillar_start_y = config.getInt("TestPillar.start_y");
        } else {
            testpillar_start_y = null;
        }
        testpillar_height = config.getInt("TestPillar.height", 0);
        testpillar_unbreakable = config.getBoolean("TestPillar.unbreakable", true);
        testpillar_radius = config.getDouble("TestPillar.radius", 3.0);
        testpillar_thickness = config.getDouble("TestPillar.thickness", 1.0);
        testpillar_layers_per_tick = config.getInt("TestPillar.layers_per_tick", 4);
        testpillar_gateway_chance = config.getDouble("TestPillar.gateway_chance", 0.15);
        testpillar_block_type = config.getString("TestPillar.block_type", "END_GATEWAY");
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
                getConfig().set("TestPillar.x", 0);
                getConfig().set("TestPillar.z", 0);
                // leave start_y absent to mean world min height
                getConfig().set("TestPillar.height", 0);
                getConfig().set("TestPillar.unbreakable", true);
                getConfig().set("TestPillar.radius", 3.0);
                getConfig().set("TestPillar.thickness", 1.0);
                getConfig().set("TestPillar.layers_per_tick", 4);
                getConfig().set("TestPillar.gateway_chance", 0.15);
                getConfig().set("TestPillar.block_type", "END_GATEWAY");
                saveConfig();
            } else {
                boolean updated = false;
                if(!text.contains("layers_per_tick")) {
                    getConfig().set("TestPillar.layers_per_tick", 4);
                    updated = true;
                }
                if(!text.contains("gateway_chance")) {
                    getConfig().set("TestPillar.gateway_chance", 0.15);
                    updated = true;
                }
                if(!text.contains("block_type")) {
                    getConfig().set("TestPillar.block_type", "END_GATEWAY");
                    updated = true;
                }
                if(updated) {
                    saveConfig();
                }
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
    public int getTestPillarX() { return testpillar_x; }
    public int getTestPillarZ() { return testpillar_z; }
    public Integer getTestPillarStartY() { return testpillar_start_y; }
    public int getTestPillarHeight() { return testpillar_height; }
    public boolean isTestPillarUnbreakable() { return testpillar_unbreakable; }
    public double getTestPillarRadius() { return testpillar_radius; }
    public double getTestPillarThickness() { return testpillar_thickness; }
    public int getTestPillarLayersPerTick() { return testpillar_layers_per_tick; }
    public double getTestPillarGatewayChance() { return testpillar_gateway_chance; }
    public String getTestPillarBlockType() { return testpillar_block_type; }

    public boolean isCustomDarkWitherSummonEnabled() {
        return custom_dark_wither_summon_enabled;
    }
}
