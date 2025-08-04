package vct.voiidstudios.managers;

import org.bukkit.configuration.file.FileConfiguration;
import vct.voiidstudios.VoiidCountdownTimer;
import vct.voiidstudios.utils.Formatter;

public class MainConfigManager {
    private CustomConfig configFile;
    private VoiidCountdownTimer plugin;


    private boolean update_notification;
    private int ticks_hide_after_ending;
    private String bossbar_default_color;
    private String timer_sound;
    private String timer_bossbar_text;
    private String text_format;
    private int refresh_ticks;

    private String commandReload;
    private String commandNoPermissions;
    private String debugEnabled;
    private String debugDisabled;
    private String timerSetError;
    private String timerSetFormatIncorrect;
    private String timerSetFormatInvalid;
    private String timerSetFormatOutRange;
    private String timerModifyError;
    private String timerModifyInvalid;
    private String timerModifyAddError;
    private String timerModifyAdd;
    private String timerModifySetError;
    private String timerModifySet;
    private String timerModifyTakeError;
    private String timerModifyTake;
    private String timerModifyBossbarError;
    private String timerModifyBossbar;
    private String timerStart;
    private String timerPause;
    private String timerResume;
    private String timerStop;
    private String timerDontExists;


    public MainConfigManager(VoiidCountdownTimer plugin){
        this.plugin = plugin;
        configFile = new CustomConfig("config.yml", null, plugin, false);
        configFile.registerConfig();
        loadConfig();
    }

    public void loadConfig(){
        FileConfiguration config = configFile.getConfig();

        update_notification = config.getBoolean("Config.update_notification");
        ticks_hide_after_ending = config.getInt("Config.ticks_hide_after_ending");
        bossbar_default_color = config.getString("Config.bossbar_default_color");
        timer_sound = config.getString("Config.timer_sound");
        timer_bossbar_text = config.getString("Config.timer_bossbar_text");
        text_format = config.getString("Config.text_format");
        refresh_ticks = config.getInt("Config.refresh_ticks");

        commandReload = config.getString("Messages.commandReload");
        commandNoPermissions = config.getString("Messages.commandNoPermissions");
        debugEnabled = config.getString("Messages.debugEnabled");
        debugDisabled = config.getString("Messages.debugDisabled");
        timerSetError = config.getString("Messages.timerSetError");
        timerSetFormatIncorrect = config.getString("Messages.timerSetFormatIncorrect");
        timerSetFormatInvalid = config.getString("Messages.timerSetFormatInvalid");
        timerSetFormatOutRange = config.getString("Messages.timerSetFormatOutRange");
        timerModifyError = config.getString("Messages.timerModifyError");
        timerModifyInvalid = config.getString("Messages.timerModifyInvalid");
        timerModifyAddError = config.getString("Messages.timerModifyAddError");
        timerModifyAdd = config.getString("Messages.timerModifyAdd");
        timerModifySetError = config.getString("Messages.timerModifySetError");
        timerModifySet = config.getString("Messages.timerModifySet");
        timerModifyTakeError = config.getString("Messages.timerModifyTakeError");
        timerModifyTake = config.getString("Messages.timerModifyTake");
        timerModifyBossbarError = config.getString("Messages.timerModifyBossbarError");
        timerModifyBossbar = config.getString("Messages.timerModifyBossbar");
        timerStart = config.getString("Messages.timerStart");
        timerPause = config.getString("Messages.timerPause");
        timerResume = config.getString("Messages.timerResume");
        timerStop = config.getString("Messages.timerStop");
        timerDontExists = config.getString("Messages.timerDontExists");
    }

    public void reloadConfig(){
        configFile.reloadConfig();
        loadConfig();
    }

    public String getTimer_bossbar_text() {
        return timer_bossbar_text;
    }

    public boolean isUpdate_notification() {
        return update_notification;
    }

    public int getTicks_hide_after_ending() {
        return ticks_hide_after_ending;
    }

    public String getBossbar_default_color() {
        return bossbar_default_color;
    }

    public String getTimer_sound() {
        return timer_sound;
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

    public String getCommandReload() {
        return commandReload;
    }

    public String getCommandNoPermissions() {
        return commandNoPermissions;
    }

    public String getDebugEnabled() {
        return debugEnabled;
    }

    public String getDebugDisabled() {
        return debugDisabled;
    }

    public String getTimerSetError() {
        return timerSetError;
    }

    public String getTimerSetFormatIncorrect() {
        return timerSetFormatIncorrect;
    }

    public String getTimerSetFormatInvalid() {
        return timerSetFormatInvalid;
    }

    public String getTimerSetFormatOutRange() {
        return timerSetFormatOutRange;
    }

    public String getTimerModifyError() {
        return timerModifyError;
    }

    public String getTimerModifyInvalid() {
        return timerModifyInvalid;
    }

    public String getTimerModifyBossbar() {
        return timerModifyBossbar;
    }

    public String getTimerModifyBossbarError() {
        return timerModifyBossbarError;
    }

    public String getTimerModifyTake() {
        return timerModifyTake;
    }

    public String getTimerModifyTakeError() {
        return timerModifyTakeError;
    }

    public String getTimerModifySet() {
        return timerModifySet;
    }

    public String getTimerModifySetError() {
        return timerModifySetError;
    }

    public String getTimerModifyAdd() {
        return timerModifyAdd;
    }

    public String getTimerModifyAddError() {
        return timerModifyAddError;
    }

    public String getTimerStart() {
        return timerStart;
    }

    public String getTimerPause() {
        return timerPause;
    }

    public String getTimerStop() {
        return timerStop;
    }

    public String getTimerResume() {
        return timerResume;
    }

    public String getTimerDontExists() {
        return timerDontExists;
    }
}
