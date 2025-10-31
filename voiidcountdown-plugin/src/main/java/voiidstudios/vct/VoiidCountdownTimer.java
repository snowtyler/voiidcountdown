package voiidstudios.vct;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import voiidstudios.vct.api.Metrics;
import voiidstudios.vct.api.PAPIExpansion;
import voiidstudios.vct.commands.MainCommand;
import voiidstudios.vct.configs.ConfigsManager;
import voiidstudios.vct.listeners.PlayerListener;
import voiidstudios.vct.listeners.EnderDragonListener;
import voiidstudios.vct.listeners.CustomSummonListener;
import voiidstudios.vct.listeners.DarkWitherDeathListener;
import voiidstudios.vct.listeners.DarkWitherSpawnListener;
import voiidstudios.vct.listeners.SpawnBookInventoryListener;
import voiidstudios.vct.managers.DependencyManager;
import voiidstudios.vct.api.UpdateCheckerResult;
import voiidstudios.vct.managers.DynamicsManager;
import voiidstudios.vct.managers.MessagesManager;
import voiidstudios.vct.managers.HalloweenModeManager;
import voiidstudios.vct.managers.TimerStateManager;
 
import voiidstudios.vct.managers.VisualBlockManager;
import voiidstudios.vct.managers.FreezeManager;
import voiidstudios.vct.utils.ServerVersion;
import voiidstudios.vct.utils.UpdateChecker;
import voiidstudios.vct.challenges.ChallengeManager;
import voiidstudios.vct.listeners.FreezeListener;

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
    private static MessagesManager messagesManager;
    private static TimerStateManager timerStateManager;
    private static DependencyManager dependencyManager;
    private static HalloweenModeManager halloweenModeManager;
    
    // Lectern protection
    private static voiidstudios.vct.managers.LecternProtectionManager lecternProtectionManager;
    private static ChallengeManager challengeManager;
    private static VisualBlockManager visualBlockManager;
    private static FreezeManager freezeManager;
    private static voiidstudios.vct.managers.InteractionActionManager interactionActionManager;
    private static voiidstudios.vct.managers.ItemFrameSensorManager itemFrameSensorManager;
    private static voiidstudios.vct.managers.SpawnBookManager spawnBookManager;

    public void onEnable() {
        instance = this;
        configsManager = new ConfigsManager(this);
        messagesManager = new MessagesManager(this);
        configsManager.configure();
        // Load prefix (and use_prefix) from config
        try {
            String cfgPrefix = configsManager.getMainConfigManager().getConfig().getString("Messages.prefix", prefix);
            boolean usePref = configsManager.getMainConfigManager().isUsePrefix();
            prefix = (usePref && cfgPrefix != null) ? cfgPrefix : "";
        } catch (Throwable ignored) {}
        halloweenModeManager = new HalloweenModeManager(this);
        // Initialize lectern protection manager
        lecternProtectionManager = new voiidstudios.vct.managers.LecternProtectionManager(this);
        challengeManager = new ChallengeManager(this);
        // Initialize spawn book manager for building/updating prophecy books (no auto-give)
        spawnBookManager = new voiidstudios.vct.managers.SpawnBookManager(this);
        visualBlockManager = new VisualBlockManager(this);
        freezeManager = new FreezeManager(this);
        interactionActionManager = new voiidstudios.vct.managers.InteractionActionManager(this);
        itemFrameSensorManager = new voiidstudios.vct.managers.ItemFrameSensorManager(this);
        setVersion();
        registerCommands();
        registerEvents();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
        }

        MessagesManager.setPrefix(prefix);

        Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage("&6        __ ___"));
        Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage("&5  \\  / &6|    |    &dVoiid &eCountdown Timer"));
        Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage("&5   \\/  &6|__  |    &8Running v" + version + " on " + serverName + " (" + cleanVersion + ")"));
        Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(""));

        new Metrics(this, 26790);
        dependencyManager = new DependencyManager(this);
        dynamicsManager = new DynamicsManager(this);
        updateChecker = new UpdateChecker(version);

        if (configsManager.getMainConfigManager().isUpdate_notification()) {
            updateMessage(updateChecker.check());
        }

        timerStateManager = new TimerStateManager(this);
        timerStateManager.loadState();
        halloweenModeManager.reload();
        challengeManager.reload();
        // Load spawn book templates and trigger initial refresh
        try { spawnBookManager.reload(); } catch (Throwable ignored) {}
        // Load lectern protection config
        lecternProtectionManager.reload();
        // Restore freeze state if it was active before shutdown
        try {
            freezeManager.loadAndApplyPersistentState();
        } catch (Throwable ignored) {}

        // Start the item frame sensor if enabled
        try {
            itemFrameSensorManager.startIfEnabled();
        } catch (Throwable ignored) {}
    }

    public void onDisable() {
        if (halloweenModeManager != null) {
            halloweenModeManager.shutdown();
        }
        if (challengeManager != null) {
            challengeManager.shutdown();
        }
        if (freezeManager != null) {
            // Clean up effects but preserve state for restoration on next startup
            freezeManager.shutdownForDisable();
        }
        if (visualBlockManager != null) {
            visualBlockManager.restoreAll();
        }
        if (timerStateManager != null && configsManager.getMainConfigManager().isSave_state_timers()) {
            timerStateManager.saveState();
        }
        if (itemFrameSensorManager != null) {
            try { itemFrameSensorManager.shutdown(); } catch (Throwable ignored) {}
        }

        Bukkit.getConsoleSender().sendMessage(
                MessagesManager.getColoredMessage(prefix+"&aHas been disabled! Goodbye ;)")
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
        this.getCommand("voiidcountdowntimer").setExecutor(new MainCommand());
    }

    public void registerEvents() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new EnderDragonListener(this), this);
        getServer().getPluginManager().registerEvents(new voiidstudios.vct.listeners.BreakingProtectionListener(visualBlockManager), this);
        getServer().getPluginManager().registerEvents(new FreezeListener(this), this);
        getServer().getPluginManager().registerEvents(new voiidstudios.vct.listeners.InteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new voiidstudios.vct.listeners.ItemFrameSensorListener(), this);
        // Lectern listeners (copy and protection)
        getServer().getPluginManager().registerEvents(new voiidstudios.vct.listeners.LecternProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new voiidstudios.vct.listeners.LecternCopyListener(), this);
        getServer().getPluginManager().registerEvents(new SpawnBookInventoryListener(), this);
        // Register protection for the sensor's anchor and frame when enabled in config
        if (configsManager.getMainConfigManager().isItemFrameSensorEnabled() &&
            (configsManager.getMainConfigManager().isItemFrameSensorProtectAnchor() || configsManager.getMainConfigManager().isItemFrameSensorProtectFrame())) {
            getServer().getPluginManager().registerEvents(new voiidstudios.vct.listeners.ItemFrameProtectionListener(), this);
        }
        getServer().getPluginManager().registerEvents(new DarkWitherDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new DarkWitherSpawnListener(this), this);
        if (configsManager.getMainConfigManager().isCustomDarkWitherSummonEnabled()) {
            getServer().getPluginManager().registerEvents(new CustomSummonListener(), this);
            Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(prefix + "&aCustom Dark Wither summon listener enabled."));
        }
        else {
            Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(prefix + "&eCustom Dark Wither summon listener is disabled in config."));
        }
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
                Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage("&aAn update for Voiid Countdown Timer &e("+latestVersion+") &ais available."));
                Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage("&aYou can download it at: &fhttps://modrinth.com/datapack/voiid-countdown-timer"));
            }
        }else{
            Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(prefix+"&cAn error occurred while checking for updates."));
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

    public static MessagesManager getMessagesManager() {
		return messagesManager;
	}

    public static TimerStateManager getTimerStateManager() {
        return timerStateManager;
    }

    public static HalloweenModeManager getHalloweenModeManager() {
        return halloweenModeManager;
    }

    public static voiidstudios.vct.managers.LecternProtectionManager getLecternProtectionManager() { return lecternProtectionManager; }

    public static ChallengeManager getChallengeManager() {
        return challengeManager;
    }

    public static VisualBlockManager getVisualBlockManager() {
        return visualBlockManager;
    }

    public static FreezeManager getFreezeManager() {
        return freezeManager;
    }

    public static voiidstudios.vct.managers.InteractionActionManager getInteractionActionManager() {
        return interactionActionManager;
    }

    public static voiidstudios.vct.managers.ItemFrameSensorManager getItemFrameSensorManager() {
        return itemFrameSensorManager;
    }

    public static void setItemFrameSensorManager(voiidstudios.vct.managers.ItemFrameSensorManager mgr) {
        itemFrameSensorManager = mgr;
    }

    public static voiidstudios.vct.managers.SpawnBookManager getSpawnBookManager() {
        return spawnBookManager;
    }
}