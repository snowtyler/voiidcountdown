package voiidstudios.vct.configs;

import org.bukkit.boss.BarColor;
import org.bukkit.configuration.file.FileConfiguration;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.configs.model.CustomConfig;
import voiidstudios.vct.configs.model.TimerConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigsManager {
    private final MainConfigManager mainConfigManager;
    private final TimersFolderConfigManager timerFolderConfigManager;
    private final VoiidCountdownTimer plugin;

    private final Map<String, TimerConfig> timersConfigs = new LinkedHashMap<>();

    public ConfigsManager(VoiidCountdownTimer plugin){
        this.plugin = plugin;
        this.mainConfigManager = new MainConfigManager(plugin);
        this.timerFolderConfigManager = new TimersFolderConfigManager(plugin, "timers");
    }

    public void configure(){
        mainConfigManager.configure();
        timerFolderConfigManager.configure();
        configureTimers();
    }

    public boolean reload(){
        try {
            mainConfigManager.reloadConfig();
            configureTimers();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void configureTimers() {
        timersConfigs.clear();

        ArrayList<CustomConfig> vctTimers = getTimersConfigs();

        for(CustomConfig configFile : vctTimers){
            FileConfiguration config = configFile.getConfig();

            if(config.contains("Timers")){
                for (String key : config.getConfigurationSection("Timers").getKeys(false)) {
                    String base = "Timers." + key + ".";
                    boolean enabled = config.getBoolean(base + "enabled", true);
                    boolean soundEnabled = config.getBoolean(base + "sound_enabled", false);
                    String text = config.getString(base + "text", "%HH%:%MM%:%SS%");
                    String sound = config.getString(base + "sound", "UI_BUTTON_CLICK");
                    float soundVolume = (float) config.getDouble(base + "sound_volume", 1.0);
                    float soundPitch = (float) config.getDouble(base + "sound_pitch", 1.0);
                    String colorStr = config.getString(base + "bossbar_color", "WHITE");

                    BarColor color;
                    try {
                        color = BarColor.valueOf(colorStr.toUpperCase());
                    } catch (Exception e) {
                        color = BarColor.WHITE;
                    }

                    TimerConfig tc = new TimerConfig(key, text, sound, color, enabled, soundEnabled, soundVolume, soundPitch);

                    timersConfigs.put(key, tc);
                }
            }
        }
    }

    public void saveTimerConfig(TimerConfig tc) {
        for (CustomConfig configFile : getTimersConfigs()) {
            FileConfiguration config = configFile.getConfig();

            String base = "Timers." + tc.getId() + ".";
            if (config.contains(base)) {
                config.set(base + "enabled", tc.isEnabled());
                config.set(base + "sound_enabled", tc.isSoundEnabled());
                config.set(base + "text", tc.getText());
                config.set(base + "sound", tc.getSound());
                config.set(base + "sound_volume", tc.getSoundVolume());
                config.set(base + "sound_pitch", tc.getSoundPitch());
                config.set(base + "bossbar_color", tc.getColor().toString());

                configFile.saveConfig();
                break;
            }
        }
    }


    public TimerConfig getTimerConfig(String id) {
        return timersConfigs.get(id);
    }

    public Map<String, TimerConfig> getAllTimerConfigs() {
        return new LinkedHashMap<>(timersConfigs);
    }

    public ArrayList<CustomConfig> getTimersConfigs() {
        ArrayList<CustomConfig> timers = new ArrayList<>();

        timers.add(mainConfigManager.getConfigFile());
        timers.addAll(timerFolderConfigManager.getConfigs());

        return timers;
    }

    public MainConfigManager getMainConfigManager() {
        return mainConfigManager;
    }
}
