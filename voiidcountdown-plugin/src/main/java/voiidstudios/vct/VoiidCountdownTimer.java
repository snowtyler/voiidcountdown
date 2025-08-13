package voiidstudios.vct;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import voiidstudios.vct.api.Metrics;
import voiidstudios.vct.api.PAPIExpansion;
import voiidstudios.vct.commands.MainCommand;
import voiidstudios.vct.configs.ConfigsManager;
import voiidstudios.vct.listeners.PlayerListener;
import voiidstudios.vct.managers.DependencyManager;
import voiidstudios.vct.api.UpdateCheckerResult;
import voiidstudios.vct.managers.DynamicsManager;
import voiidstudios.vct.utils.MessageUtils;
import voiidstudios.vct.utils.ServerVersion;
import voiidstudios.vct.utils.UpdateChecker;

import java.util.Objects;

public final class VoiidCountdownTimer extends JavaPlugin {
    public static String prefix = "&5[&dVCT&5] ";
    public String version = getDescription().getVersion();

    private final String serverName = Bukkit.getServer().getName();
    private final String bukkitVersion = Bukkit.getBukkitVersion();
    private final String cleanVersion = bukkitVersion.split("-")[0];

    public static ServerVersion serverVersion;
    private static VoiidCountdownTimer instance;
    private UpdateChecker updateChecker;
    private static ConfigsManager configsManager;
    private static DynamicsManager dynamicsManager;
    private static Metrics bStatsMetrics;
    private static DependencyManager dependencyManager;

    public void onEnable() {
        instance = this;
        configsManager = new ConfigsManager(this);
        configsManager.configure();
        setVersion();
        registerCommands();
        registerEvents();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
        }

        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&6        __ ___"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&5  \\  / &6|    |    &dVoiid &eCountdown Timer"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&5   \\/  &6|__  |    &8Running v" + version + " on " + serverName + " (" + cleanVersion + ")"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(""));

        dependencyManager = new DependencyManager(this);
        bStatsMetrics = new Metrics(this, 26790);
        dynamicsManager = new DynamicsManager(this);
        updateChecker = new UpdateChecker(version);

        if (configsManager.getMainConfigManager().isUpdate_notification()) {
            updateMessage(updateChecker.check());
        }
    }

    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(
                MessageUtils.getColoredMessage(prefix+"&rHas been disabled! Goodbye ;)")
        );
    }

    public void setVersion(){
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        switch(bukkitVersion){
            case "1.20.5":
            case "1.20.6":
                serverVersion = ServerVersion.v1_20_R4;
                break;
            case "1.21":
            case "1.21.1":
                serverVersion = ServerVersion.v1_21_R1;
                break;
            case "1.21.2":
            case "1.21.3":
                serverVersion = ServerVersion.v1_21_R2;
                break;
            case "1.21.4":
                serverVersion = ServerVersion.v1_21_R3;
                break;
            case "1.21.5":
                serverVersion = ServerVersion.v1_21_R4;
                break;
            case "1.21.6":
            case "1.21.7":
            case "1.21.8":
                serverVersion = ServerVersion.v1_21_R5;
                break;
            default:
                try{
                    serverVersion = ServerVersion.valueOf(packageName.replace("org.bukkit.craftbukkit.", ""));
                }catch(Exception e){
                    serverVersion = ServerVersion.v1_21_R5;
                }
        }
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

    public static ConfigsManager getConfigsManager() {
        return configsManager;
    }

    public static DynamicsManager getPhasesManager() {
        return dynamicsManager;
    }

    public static DependencyManager getDependencyManager() {
        return dependencyManager;
    }
}