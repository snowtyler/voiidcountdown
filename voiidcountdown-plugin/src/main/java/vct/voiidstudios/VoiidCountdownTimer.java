package vct.voiidstudios;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import vct.voiidstudios.api.Metrics;
import vct.voiidstudios.api.PAPIExpansion;
import vct.voiidstudios.commands.MainCommand;
import vct.voiidstudios.listeners.PlayerListener;
import vct.voiidstudios.managers.MainConfigManager;
import vct.voiidstudios.api.UpdateCheckerResult;
import vct.voiidstudios.managers.PhasesManager;
import vct.voiidstudios.utils.MessageUtils;
import vct.voiidstudios.utils.UpdateChecker;

import java.util.Objects;

public final class VoiidCountdownTimer extends JavaPlugin {
    public static String prefix = "&5[&dVCT&5] ";
    public String version = getDescription().getVersion();

    private static VoiidCountdownTimer instance;
    private UpdateChecker updateChecker;
    private static MainConfigManager mainConfigManager;
    private static PhasesManager phasesManager;
    private static Metrics bStatsMetrics;

    public void onEnable() {
        instance = this;
        mainConfigManager = new MainConfigManager(this);
        registerCommands();
        registerEvents();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
        }

        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&6        __ ___"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&5  \\  / &6|    |    &dVoiid &eCountdown Timer"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&5   \\/  &6|__  |    &8Running v"+version));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(""));

        bStatsMetrics = new Metrics(this, 26790);
        phasesManager = new PhasesManager(this);
        updateChecker = new UpdateChecker(version);

        if (mainConfigManager.isUpdate_notification()) {
            updateMessage(updateChecker.check());
        }
    }

    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', prefix+"&rHas been disabled! Goodbye ;)")
        );
    }

    public void registerCommands() {
        Objects.requireNonNull(this.getCommand("voiidcountdowntimer")).setExecutor(new MainCommand());
    }

    public void registerEvents() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    public static VoiidCountdownTimer getInstance() {
        return instance;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public void updateMessage(UpdateCheckerResult result){
        if(!result.isError()){
            String latestVersion = result.getLatestVersion();
            if(latestVersion != null){
                Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&aAn update for Voiid Countdown Timer &e("+latestVersion+") &ais available."));
                Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&aYou can download it at: &fhttps://modrinth.com/datapack/voiid-countdown-timer"));
            }
        }else{
            Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix+"&cAn error occurred while checking for updates."));
        }
    }

    public static MainConfigManager getMainConfigManager() {
        return mainConfigManager;
    }

    public static PhasesManager getPhasesManager() {
        return phasesManager;
    }
}