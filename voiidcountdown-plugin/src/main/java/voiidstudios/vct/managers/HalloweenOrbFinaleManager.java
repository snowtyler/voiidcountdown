package voiidstudios.vct.managers;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.configs.model.CustomConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class HalloweenOrbFinaleManager {
    private static final double MIN_RADIUS = 0.5D;
    private static final double MIN_EXPANSION = 0.01D;
    private static final double STOP_EPSILON = 1.0E-3D;
    private static final double BORDER_MAX_DIAMETER = 5.9999968E7D;

    private final VoiidCountdownTimer plugin;
    private final CustomConfig configFile;
    private final Object stateLock = new Object();

    private boolean enabled;
    private double startRadius;
    private double expansionPerTick;
    private double maxRadius;
    private long tickInterval;
    private double originX;
    private double originZ;
    private double fallbackOriginY;
    private double originYOffset;
    private boolean useSpawnY;
    private List<String> worldNames = new ArrayList<>();
    private double borderDamageAmount;
    private double borderDamageBuffer;
    private int borderWarningDistance;
    private int borderWarningTime;
    private String banMessage;
    private String kickMessage;
    private boolean broadcastPunishment;
    private String broadcastPunishmentMessage;
    private String broadcastStartMessage;
    private String broadcastStopMessage;
    private String broadcastPauseMessage;
    private String broadcastResumeMessage;
    private String bypassPermission;

    private FinaleVisualMode visualMode = FinaleVisualMode.WORLD_BORDER;
    private Material virtualMaterial = Material.RED_STAINED_GLASS;
    private int virtualHeight = 3;
    private int virtualDensity = 2;

    private final boolean protocolLibAvailable;

    private final Map<UUID, BorderInstance> activeBorders = new HashMap<>();
    private final Set<UUID> punishedPlayers = new HashSet<>();

    private boolean active;
    private boolean paused;
    private int punishedCount;

    public HalloweenOrbFinaleManager(VoiidCountdownTimer plugin) {
        this.plugin = plugin;
        this.configFile = new CustomConfig("halloween.yml", plugin, null, false);
        this.configFile.registerConfig();
        this.protocolLibAvailable = plugin.getServer().getPluginManager().isPluginEnabled("ProtocolLib");
        parseConfig();
    }

    public void reload() {
        synchronized (stateLock) {
            stopFinaleInternal();
            configFile.reloadConfig();
            parseConfig();
        }
    }

    public void shutdown() {
        synchronized (stateLock) {
            stopFinaleInternal();
        }
    }

    private void parseConfig() {
        FileConfiguration cfg = configFile.getConfig();
        ConfigurationSection root = cfg.getConfigurationSection("halloween_mode");
        if (root == null) {
            enabled = false;
            return;
        }

        ConfigurationSection finaleSection = root.getConfigurationSection("finale");
        if (finaleSection == null) {
            enabled = false;
            return;
        }

        ConfigurationSection orbSection = finaleSection.getConfigurationSection("orb");
        if (orbSection == null) {
            enabled = false;
            return;
        }

        enabled = orbSection.getBoolean("enabled", false);
        startRadius = Math.max(MIN_RADIUS, orbSection.getDouble("start_radius", 2.5D));
        expansionPerTick = Math.max(MIN_EXPANSION, orbSection.getDouble("expansion_per_tick", 0.35D));
        double configuredMaxRadius = orbSection.getDouble("max_radius", 0.0D);
        maxRadius = configuredMaxRadius > 0.0D ? Math.max(startRadius, configuredMaxRadius) : Double.POSITIVE_INFINITY;
        tickInterval = Math.max(1L, orbSection.getLong("tick_interval", 2L));

        ConfigurationSection originSection = orbSection.getConfigurationSection("origin");
        if (originSection != null) {
            originX = originSection.getDouble("x", 0.5D);
            originZ = originSection.getDouble("z", 0.5D);
            fallbackOriginY = originSection.getDouble("y", 64.0D);
            originYOffset = originSection.getDouble("y_offset", 0.0D);
            useSpawnY = originSection.getBoolean("use_spawn_y", true);
        } else {
            originX = 0.5D;
            originZ = 0.5D;
            fallbackOriginY = 64.0D;
            originYOffset = 0.0D;
            useSpawnY = true;
        }

        List<String> configuredWorlds = orbSection.getStringList("worlds");
        if (configuredWorlds == null || configuredWorlds.isEmpty()) {
            worldNames = new ArrayList<>();
            worldNames.add("world");
            worldNames.add("world_nether");
            worldNames.add("world_the_end");
        } else {
            List<String> sanitized = new ArrayList<>();
            for (String entry : configuredWorlds) {
                if (entry != null && !entry.trim().isEmpty()) {
                    sanitized.add(entry.trim());
                }
            }
            worldNames = sanitized;
        }

        ConfigurationSection visualSection = orbSection.getConfigurationSection("visual");
        String visualModeText = visualSection != null ? visualSection.getString("mode", "WORLD_BORDER") : "WORLD_BORDER";
        visualMode = FinaleVisualMode.fromString(visualModeText);
        if (visualMode == FinaleVisualMode.VIRTUAL_BLOCKS && !protocolLibAvailable) {
            plugin.getLogger().warning("Halloween finale configured for virtual blocks, but ProtocolLib is not available. Falling back to WORLD_BORDER mode.");
            visualMode = FinaleVisualMode.WORLD_BORDER;
        }

        if (visualSection != null) {
            String materialName = visualSection.getString("material", "RED_STAINED_GLASS");
            Material parsed = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
            if (parsed != null) {
                virtualMaterial = parsed;
            } else {
                plugin.getLogger().warning("Unknown visual material '" + materialName + "' in halloween.yml. Using RED_STAINED_GLASS.");
                virtualMaterial = Material.RED_STAINED_GLASS;
            }
            virtualHeight = Math.max(1, visualSection.getInt("height", 3));
            virtualDensity = Math.max(1, visualSection.getInt("density", 2));
        } else {
            virtualMaterial = Material.RED_STAINED_GLASS;
            virtualHeight = 3;
            virtualDensity = 2;
        }

        ConfigurationSection borderSection = orbSection.getConfigurationSection("world_border");
        borderDamageAmount = borderSection != null ? Math.max(0.0D, borderSection.getDouble("damage_amount", 1.0D)) : 1.0D;
        borderDamageBuffer = borderSection != null ? Math.max(0.0D, borderSection.getDouble("damage_buffer", 5.0D)) : 5.0D;
        borderWarningDistance = borderSection != null ? Math.max(0, borderSection.getInt("warning_distance", 10)) : 10;
        borderWarningTime = borderSection != null ? Math.max(0, borderSection.getInt("warning_time", 15)) : 15;

        ConfigurationSection punishmentSection = orbSection.getConfigurationSection("punishment");
        banMessage = punishmentSection != null ? punishmentSection.getString("ban_message", "&cThe finale border has consumed you.") : "&cThe finale border has consumed you.";
        kickMessage = punishmentSection != null ? punishmentSection.getString("kick_message", "&cEven operators cannot outrun the finale border.") : "&cEven operators cannot outrun the finale border.";
        broadcastPunishment = punishmentSection != null && punishmentSection.getBoolean("broadcast", false);
        broadcastPunishmentMessage = punishmentSection != null ? punishmentSection.getString("broadcast_message", "&5[Halloween]&c %PLAYER% was consumed by the finale border.") : "&5[Halloween]&c %PLAYER% was consumed by the finale border.";
        bypassPermission = punishmentSection != null ? punishmentSection.getString("bypass_permission", "") : "";

        ConfigurationSection broadcastSection = orbSection.getConfigurationSection("broadcast");
        broadcastStartMessage = broadcastSection != null ? broadcastSection.getString("start_message", "&5[Halloween]&f The finale border begins to expand.") : "&5[Halloween]&f The finale border begins to expand.";
        broadcastStopMessage = broadcastSection != null ? broadcastSection.getString("stop_message", "&5[Halloween]&f The finale border has faded.") : "&5[Halloween]&f The finale border has faded.";
        broadcastPauseMessage = broadcastSection != null ? broadcastSection.getString("pause_message", "&5[Halloween]&f The finale border pauses its expansion.") : "&5[Halloween]&f The finale border pauses its expansion.";
        broadcastResumeMessage = broadcastSection != null ? broadcastSection.getString("resume_message", "&5[Halloween]&f The finale border resumes its expansion.") : "&5[Halloween]&f The finale border resumes its expansion.";

        synchronized (stateLock) {
            if (!enabled) {
                stopFinaleInternal();
            }
        }
    }

    public FinaleActionResult startFinale(String source) {
        synchronized (stateLock) {
            if (!enabled) {
                return FinaleActionResult.failure("&cFinale border is disabled in halloween.yml.");
            }
            if (active) {
                return FinaleActionResult.failure("&eFinale border is already running.");
            }

            List<World> targets = resolveWorlds();
            if (targets.isEmpty()) {
                return FinaleActionResult.failure("&cNo configured finale worlds are currently loaded.");
            }

            active = true;
            paused = false;
            punishedPlayers.clear();
            punishedCount = 0;

            for (World world : targets) {
                BorderInstance instance = new BorderInstance(world);
                instance.start();
                activeBorders.put(world.getUID(), instance);
            }

            if (broadcastStartMessage != null && !broadcastStartMessage.trim().isEmpty()) {
                Bukkit.broadcastMessage(MessagesManager.getColoredMessage(broadcastStartMessage));
            }

            plugin.getLogger().info("Halloween finale border started by " + (source != null ? source : "unknown trigger") + ".");
            return FinaleActionResult.success("&aFinale border engaged.");
        }
    }

    public FinaleActionResult stopFinale(String source) {
        synchronized (stateLock) {
            if (!active) {
                return FinaleActionResult.failure("&eFinale border is not currently running.");
            }

            stopFinaleInternal();

            if (broadcastStopMessage != null && !broadcastStopMessage.trim().isEmpty()) {
                Bukkit.broadcastMessage(MessagesManager.getColoredMessage(broadcastStopMessage));
            }

            plugin.getLogger().info("Halloween finale border stopped by " + (source != null ? source : "unknown trigger") + ".");
            return FinaleActionResult.success("&aFinale border halted.");
        }
    }

    public FinaleActionResult pauseFinale(String source) {
        synchronized (stateLock) {
            if (!active || activeBorders.isEmpty()) {
                return FinaleActionResult.failure("&eFinale border is not currently running.");
            }
            if (paused) {
                return FinaleActionResult.failure("&eFinale border is already paused.");
            }

            paused = true;
            for (BorderInstance instance : activeBorders.values()) {
                instance.pause();
            }

            if (broadcastPauseMessage != null && !broadcastPauseMessage.trim().isEmpty()) {
                Bukkit.broadcastMessage(MessagesManager.getColoredMessage(broadcastPauseMessage));
            }

            plugin.getLogger().info("Halloween finale border paused by " + (source != null ? source : "unknown trigger") + ".");
            return FinaleActionResult.success("&aFinale border paused.");
        }
    }

    public FinaleActionResult resumeFinale(String source) {
        synchronized (stateLock) {
            if (!active || activeBorders.isEmpty()) {
                return FinaleActionResult.failure("&eFinale border is not currently running.");
            }
            if (!paused) {
                return FinaleActionResult.failure("&eFinale border is not paused.");
            }

            paused = false;
            for (BorderInstance instance : activeBorders.values()) {
                instance.resume();
            }

            if (broadcastResumeMessage != null && !broadcastResumeMessage.trim().isEmpty()) {
                Bukkit.broadcastMessage(MessagesManager.getColoredMessage(broadcastResumeMessage));
            }

            plugin.getLogger().info("Halloween finale border resumed by " + (source != null ? source : "unknown trigger") + ".");
            return FinaleActionResult.success("&aFinale border resumed.");
        }
    }

    private void stopFinaleInternal() {
        for (BorderInstance instance : activeBorders.values()) {
            instance.shutdown(true);
        }
        activeBorders.clear();
        active = false;
        paused = false;
        punishedPlayers.clear();
    }

    public OrbStatus getStatus() {
        synchronized (stateLock) {
            List<OrbWorldStatus> statuses = new ArrayList<>();
            for (String configured : worldNames) {
                if (configured == null || configured.trim().isEmpty()) {
                    continue;
                }
                String name = configured.trim();
                World world = plugin.getServer().getWorld(name);
                WorldBorder border = world != null ? world.getWorldBorder() : null;
                BorderInstance instance = (world != null) ? activeBorders.get(world.getUID()) : null;
                boolean running = instance != null && instance.isRunning();
                boolean pausedInstance = instance != null && instance.isPaused();
                double radius = border != null ? border.getSize() / 2.0D : 0.0D;
                double trackedRadius = instance != null ? instance.getRadius() : radius;
                double centerX = border != null ? border.getCenter().getX() : originX;
                double centerZ = border != null ? border.getCenter().getZ() : originZ;
                String environment = world != null ? world.getEnvironment().name() : "UNLOADED";
                statuses.add(new OrbWorldStatus(name, environment, running, pausedInstance, trackedRadius, radius, centerX, centerZ));
            }
            return new OrbStatus(
                    enabled,
                    active,
                    paused,
                    startRadius,
                    expansionPerTick,
                    maxRadius,
                    tickInterval,
                    borderDamageAmount,
                    borderDamageBuffer,
                    borderWarningDistance,
                    borderWarningTime,
                    Collections.unmodifiableList(statuses),
                    punishedCount);
        }
    }

    private List<World> resolveWorlds() {
        List<World> worlds = new ArrayList<>();
        for (String configured : worldNames) {
            if (configured == null || configured.trim().isEmpty()) {
                continue;
            }
            World world = plugin.getServer().getWorld(configured.trim());
            if (world != null) {
                worlds.add(world);
            } else {
                plugin.getLogger().warning("Configured finale world '" + configured + "' is not loaded.");
            }
        }
        return worlds;
    }

    private Location computeCenter(World world) {
        double y = useSpawnY ? world.getSpawnLocation().getY() : fallbackOriginY;
        y += originYOffset;
        return new Location(world, originX, y, originZ);
    }

    private void checkPlayers(World world, WorldBorder border) {
        if (world == null || border == null) {
            return;
        }
        for (Player player : world.getPlayers()) {
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (shouldBypass(player)) {
                continue;
            }
            if (border.isInside(player.getLocation())) {
                registerPunishment(player);
            }
        }
    }

    private void registerPunishment(Player player) {
        String name = player.getName();
        UUID uuid = player.getUniqueId();
        boolean alreadyHandled;
        synchronized (stateLock) {
            alreadyHandled = !punishedPlayers.add(uuid);
            if (!alreadyHandled) {
                punishedCount++;
            }
        }
        if (alreadyHandled) {
            return;
        }

        boolean isOp = player.isOp();
        String message = isOp ? kickMessage : banMessage;
        String colored = MessagesManager.getColoredMessage(message != null ? message : "&cConsumed by the finale border.");

        if (isOp) {
            player.kickPlayer(colored);
            plugin.getLogger().info("Finale border kicked operator " + name + ".");
        } else {
            Bukkit.getBanList(BanList.Type.NAME).addBan(name, colored, null, "Halloween Finale Border");
            player.kickPlayer(colored);
            plugin.getLogger().info("Finale border banned player " + name + ".");
        }

        if (broadcastPunishment && broadcastPunishmentMessage != null && !broadcastPunishmentMessage.trim().isEmpty()) {
            String raw = broadcastPunishmentMessage.replace("%PLAYER%", name);
            Bukkit.broadcastMessage(MessagesManager.getColoredMessage(raw));
        }
    }

    private boolean shouldBypass(Player player) {
        if (player == null) {
            return true;
        }
        if (bypassPermission != null) {
            String permission = bypassPermission.trim();
            if (!permission.isEmpty() && player.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    private void handleInstanceFinished(UUID worldId) {
        BorderInstance instance;
        synchronized (stateLock) {
            instance = activeBorders.remove(worldId);
            if (instance != null) {
                instance.shutdown(true);
            }
            if (activeBorders.isEmpty() && active) {
                active = false;
                paused = false;
                if (broadcastStopMessage != null && !broadcastStopMessage.trim().isEmpty()) {
                    Bukkit.broadcastMessage(MessagesManager.getColoredMessage(broadcastStopMessage));
                }
                plugin.getLogger().info("Halloween finale border completed in all configured worlds.");
            }
        }
    }

    private void applyBorderSettings(WorldBorder border) {
        if (border == null) {
            return;
        }
        border.setDamageAmount(borderDamageAmount);
        border.setDamageBuffer(borderDamageBuffer);
        border.setWarningDistance(borderWarningDistance);
        border.setWarningTime(borderWarningTime);
    }

    private static double clampRadius(double radius) {
        double maxRadiusValue = BORDER_MAX_DIAMETER * 0.5D;
        return Math.max(MIN_RADIUS, Math.min(radius, maxRadiusValue));
    }

    private enum FinaleVisualMode {
        WORLD_BORDER,
        VIRTUAL_BLOCKS;

        static FinaleVisualMode fromString(String value) {
            if (value == null) {
                return WORLD_BORDER;
            }
            for (FinaleVisualMode mode : values()) {
                if (mode.name().equalsIgnoreCase(value.trim())) {
                    return mode;
                }
            }
            return WORLD_BORDER;
        }
    }

    public static final class FinaleActionResult {
        private final boolean success;
        private final String message;

        private FinaleActionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static FinaleActionResult success(String message) {
            return new FinaleActionResult(true, message);
        }

        public static FinaleActionResult failure(String message) {
            return new FinaleActionResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class OrbStatus {
        private final boolean enabled;
        private final boolean active;
        private final boolean paused;
        private final double startRadius;
        private final double expansionPerTick;
        private final double maxRadius;
        private final long tickInterval;
        private final double borderDamageAmount;
        private final double borderDamageBuffer;
        private final int borderWarningDistance;
        private final int borderWarningTime;
        private final List<OrbWorldStatus> worlds;
        private final int punishedCount;

        private OrbStatus(boolean enabled,
                          boolean active,
                          boolean paused,
                          double startRadius,
                          double expansionPerTick,
                          double maxRadius,
                          long tickInterval,
                          double borderDamageAmount,
                          double borderDamageBuffer,
                          int borderWarningDistance,
                          int borderWarningTime,
                          List<OrbWorldStatus> worlds,
                          int punishedCount) {
            this.enabled = enabled;
            this.active = active;
            this.paused = paused;
            this.startRadius = startRadius;
            this.expansionPerTick = expansionPerTick;
            this.maxRadius = maxRadius;
            this.tickInterval = tickInterval;
            this.borderDamageAmount = borderDamageAmount;
            this.borderDamageBuffer = borderDamageBuffer;
            this.borderWarningDistance = borderWarningDistance;
            this.borderWarningTime = borderWarningTime;
            this.worlds = worlds;
            this.punishedCount = punishedCount;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isActive() {
            return active;
        }

        public boolean isPaused() {
            return paused;
        }

        public double getStartRadius() {
            return startRadius;
        }

        public double getExpansionPerTick() {
            return expansionPerTick;
        }

        public double getMaxRadius() {
            return maxRadius;
        }

        public long getTickInterval() {
            return tickInterval;
        }

        public double getBorderDamageAmount() {
            return borderDamageAmount;
        }

        public double getBorderDamageBuffer() {
            return borderDamageBuffer;
        }

        public int getBorderWarningDistance() {
            return borderWarningDistance;
        }

        public int getBorderWarningTime() {
            return borderWarningTime;
        }

        public List<OrbWorldStatus> getWorlds() {
            return worlds;
        }

        public int getPunishedCount() {
            return punishedCount;
        }
    }

    public static final class OrbWorldStatus {
        private final String worldName;
        private final String environment;
        private final boolean running;
        private final boolean paused;
        private final double trackedRadius;
        private final double borderRadius;
        private final double centerX;
        private final double centerZ;

        private OrbWorldStatus(String worldName,
                               String environment,
                               boolean running,
                               boolean paused,
                               double trackedRadius,
                               double borderRadius,
                               double centerX,
                               double centerZ) {
            this.worldName = worldName;
            this.environment = environment;
            this.running = running;
            this.paused = paused;
            this.trackedRadius = trackedRadius;
            this.borderRadius = borderRadius;
            this.centerX = centerX;
            this.centerZ = centerZ;
        }

        public String getWorldName() {
            return worldName;
        }

        public String getEnvironment() {
            return environment;
        }

        public boolean isRunning() {
            return running;
        }

        public boolean isPaused() {
            return paused;
        }

        public double getTrackedRadius() {
            return trackedRadius;
        }

        public double getBorderRadius() {
            return borderRadius;
        }

        public double getCenterX() {
            return centerX;
        }

        public double getCenterZ() {
            return centerZ;
        }
    }

    private final class BorderInstance implements Runnable {
        private final World world;
        private final WorldBorder border;
        private final WorldBorderState originalBorder;
        private final double centerX;
        private final double centerZ;
        private final double expansionStep;
        private final double targetRadius;
        private final FinaleVisualMode instanceVisualMode;
        private final VirtualBorderRenderer virtualRenderer;
        private BukkitTask task;
        private double radius;
        private boolean running;
        private boolean pausedInstance;
        private BorderPlayerListener playerListener;

        private BorderInstance(World world) {
            this.world = Objects.requireNonNull(world, "world");
            this.border = world.getWorldBorder();
            this.originalBorder = WorldBorderState.capture(border);

            Location center = computeCenter(world);
            this.centerX = center.getX();
            this.centerZ = center.getZ();

            this.expansionStep = HalloweenOrbFinaleManager.this.expansionPerTick;
            this.instanceVisualMode = HalloweenOrbFinaleManager.this.visualMode;
            this.radius = clampRadius(HalloweenOrbFinaleManager.this.startRadius);

            double configuredMax = HalloweenOrbFinaleManager.this.maxRadius;
            if (Double.isFinite(configuredMax)) {
                double desired = Math.max(this.radius, configuredMax);
                this.targetRadius = clampRadius(desired);
            } else {
                this.targetRadius = Double.POSITIVE_INFINITY;
            }

            if (instanceVisualMode == FinaleVisualMode.VIRTUAL_BLOCKS) {
                this.virtualRenderer = new VirtualBorderRenderer(
                        HalloweenOrbFinaleManager.this.plugin,
                        world,
                        centerX,
                        centerZ,
                        HalloweenOrbFinaleManager.this.virtualHeight,
                        HalloweenOrbFinaleManager.this.virtualDensity,
                        HalloweenOrbFinaleManager.this.virtualMaterial
                );
            } else {
                this.virtualRenderer = null;
            }
        }

        private void start() {
            if (running) {
                return;
            }
            running = true;
            pausedInstance = false;

            if (instanceVisualMode == FinaleVisualMode.WORLD_BORDER) {
                HalloweenOrbFinaleManager.this.applyBorderSettings(border);
                border.setCenter(centerX, centerZ);
                border.setSize(radius * 2.0D);
            } else if (virtualRenderer != null) {
                virtualRenderer.render(radius);
            }

            task = HalloweenOrbFinaleManager.this.plugin.getServer().getScheduler().runTaskTimer(
                    HalloweenOrbFinaleManager.this.plugin,
                    this,
                    HalloweenOrbFinaleManager.this.tickInterval,
                    HalloweenOrbFinaleManager.this.tickInterval
            );

            if (virtualRenderer != null) {
                playerListener = new BorderPlayerListener();
                Bukkit.getPluginManager().registerEvents(playerListener, HalloweenOrbFinaleManager.this.plugin);
            }
        }

        private void pause() {
            pausedInstance = true;
        }

        private void resume() {
            if (!running) {
                return;
            }
            pausedInstance = false;

            if (instanceVisualMode == FinaleVisualMode.WORLD_BORDER) {
                border.setSize(radius * 2.0D);
            } else if (virtualRenderer != null) {
                virtualRenderer.render(radius);
            }
        }

        private void shutdown(boolean restoreBorder) {
            running = false;
            pausedInstance = false;
            if (task != null) {
                task.cancel();
                task = null;
            }
            if (restoreBorder && originalBorder != null && instanceVisualMode == FinaleVisualMode.WORLD_BORDER) {
                originalBorder.apply(border);
            }
            if (virtualRenderer != null) {
                virtualRenderer.clearAll();
            }
            if (playerListener != null) {
                HandlerList.unregisterAll(playerListener);
                playerListener = null;
            }
        }

        private boolean isRunning() {
            return running;
        }

        private boolean isPaused() {
            return pausedInstance;
        }

        private double getRadius() {
            return radius;
        }

        private boolean usesWorldBorder() {
            return instanceVisualMode == FinaleVisualMode.WORLD_BORDER;
        }

        @Override
        public void run() {
            if (!running || pausedInstance) {
                return;
            }

            double nextRadius = radius + expansionStep;
            boolean reachedTarget = Double.isFinite(targetRadius) && nextRadius >= targetRadius - STOP_EPSILON;
            radius = reachedTarget ? clampRadius(targetRadius) : clampRadius(nextRadius);

            if (instanceVisualMode == FinaleVisualMode.WORLD_BORDER) {
                border.setSize(radius * 2.0D);
            } else if (virtualRenderer != null) {
                virtualRenderer.render(radius);
            }

            punishPlayers(radius);

            if (reachedTarget) {
                running = false;
                if (task != null) {
                    task.cancel();
                    task = null;
                }
                HalloweenOrbFinaleManager.this.handleInstanceFinished(world.getUID());
            }
        }

        private void punishPlayers(double currentRadius) {
            for (Player player : world.getPlayers()) {
                if (player == null || !player.isOnline()) {
                    continue;
                }
                if (HalloweenOrbFinaleManager.this.shouldBypass(player)) {
                    continue;
                }
                boolean inside = instanceVisualMode == FinaleVisualMode.WORLD_BORDER
                        ? border.isInside(player.getLocation())
                        : isInsideVirtual(player.getLocation(), currentRadius);
                if (inside) {
                    HalloweenOrbFinaleManager.this.registerPunishment(player);
                }
            }
        }

        // Cylinder check (XZ-only) for virtual mode: players are inside if their
        // horizontal distance from center is <= currentRadius.
        private boolean isInsideVirtual(Location location, double currentRadius) {
            if (location == null) {
                return false;
            }
            double dx = location.getX() - centerX;
            double dz = location.getZ() - centerZ;
            double radiusSquared = currentRadius * currentRadius;
            return dx * dx + dz * dz <= radiusSquared;
        }

        private final class BorderPlayerListener implements Listener {
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onPlayerJoin(PlayerJoinEvent event) {
                if (virtualRenderer == null) {
                    return;
                }
                Player player = event.getPlayer();
                if (player.getWorld() != world) {
                    return;
                }
                Bukkit.getScheduler().runTaskLater(HalloweenOrbFinaleManager.this.plugin, () -> virtualRenderer.syncPlayer(player), 1L);
            }

            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
                if (virtualRenderer == null) {
                    return;
                }
                Player player = event.getPlayer();
                if (player.getWorld() == world) {
                    Bukkit.getScheduler().runTaskLater(HalloweenOrbFinaleManager.this.plugin, () -> virtualRenderer.syncPlayer(player), 1L);
                } else {
                    virtualRenderer.forgetPlayer(player.getUniqueId());
                }
            }

            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onPlayerQuit(PlayerQuitEvent event) {
                if (virtualRenderer == null) {
                    return;
                }
                virtualRenderer.forgetPlayer(event.getPlayer().getUniqueId());
            }
        }
    }

    private static final class WorldBorderState {
        private final double centerX;
        private final double centerZ;
        private final double size;
        private final double damageAmount;
        private final double damageBuffer;
        private final int warningDistance;
        private final int warningTime;

        private WorldBorderState(double centerX,
                                 double centerZ,
                                 double size,
                                 double damageAmount,
                                 double damageBuffer,
                                 int warningDistance,
                                 int warningTime) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.size = size;
            this.damageAmount = damageAmount;
            this.damageBuffer = damageBuffer;
            this.warningDistance = warningDistance;
            this.warningTime = warningTime;
        }

        private static WorldBorderState capture(WorldBorder border) {
            if (border == null) {
                return null;
            }
            Location center = border.getCenter();
            return new WorldBorderState(
                    center.getX(),
                    center.getZ(),
                    border.getSize(),
                    border.getDamageAmount(),
                    border.getDamageBuffer(),
                    border.getWarningDistance(),
                    border.getWarningTime()
            );
        }

        private void apply(WorldBorder border) {
            if (border == null) {
                return;
            }
            border.setCenter(centerX, centerZ);
            border.setSize(size);
            border.setDamageAmount(damageAmount);
            border.setDamageBuffer(damageBuffer);
            border.setWarningDistance(warningDistance);
            border.setWarningTime(warningTime);
        }
    }
}
