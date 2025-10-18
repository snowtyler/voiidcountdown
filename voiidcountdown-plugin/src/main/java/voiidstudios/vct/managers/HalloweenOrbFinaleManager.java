package voiidstudios.vct.managers;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.configs.model.CustomConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class HalloweenOrbFinaleManager {
	private static final double MIN_RADIUS = 0.5D;
	private static final double MIN_EXPANSION = 0.01D;
	private static final double STOP_EPSILON = 1.0E-3D;

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
	private Material orbMaterial;
	private int blocksPerTick;
	private int initialBurstBlocks;
	private String banMessage;
	private String kickMessage;
	private boolean broadcastPunishment;
	private String broadcastPunishmentMessage;
	private String broadcastStartMessage;
	private String broadcastStopMessage;
	private String bypassPermission;

	private final Map<UUID, OrbInstance> activeOrbs = new HashMap<>();
	private final Set<UUID> punishedPlayers = new HashSet<>();

	private boolean active;
	private int punishedCount;
	// Adapter used to place blocks efficiently (WorldEdit/FAWE or Bukkit fallback)
	private voiidstudios.vct.integrations.worldedit.BlockPlacementAdapter blockPlacementAdapter;

	public HalloweenOrbFinaleManager(VoiidCountdownTimer plugin) {
		this.plugin = plugin;
		this.configFile = new CustomConfig("halloween.yml", plugin, null, false);
		this.configFile.registerConfig();
		// detect FAWE/WorldEdit and choose adapter
		// detect FAWE/WorldEdit and choose adapter
		boolean fawePresent = false;
		try {
			fawePresent = detectFawe();
		} catch (Throwable t) {
			plugin.getLogger().warning("Error while detecting FAWE/WorldEdit: " + t.getMessage());
		}
		if (fawePresent) {
			plugin.getLogger().info("FAWE/WorldEdit detected — using WorldEdit block placement adapter for finale orb.");
			this.blockPlacementAdapter = new voiidstudios.vct.integrations.worldedit.WorldEditBlockPlacementAdapter(plugin.getLogger());
		} else {
			plugin.getLogger().info("FAWE/WorldEdit not detected — using Bukkit block placement adapter for finale orb.");
			this.blockPlacementAdapter = new voiidstudios.vct.integrations.worldedit.BukkitBlockPlacementAdapter();
		}
		parseConfig();
	}

    private boolean detectFawe() {
        // 1) PluginManager lookup
        org.bukkit.plugin.PluginManager pm = plugin.getServer().getPluginManager();
        org.bukkit.plugin.Plugin fawePlugin = pm.getPlugin("FastAsyncWorldEdit");
        if (fawePlugin != null && fawePlugin.isEnabled()) {
            plugin.getLogger().info("Detected FastAsyncWorldEdit plugin: " + fawePlugin.getDescription().getVersion());
            return true;
        }

        // 2) Classpath checks for FAWE or WorldEdit
        try {
            Class.forName("com.fastasyncworldedit.core.Fawe");
            plugin.getLogger().info("Detected FAWE on classpath via com.fastasyncworldedit.core.Fawe.");
            return true;
        } catch (ClassNotFoundException ignored) {
        }

        try {
            Class.forName("com.sk89q.worldedit.WorldEdit");
            // WorldEdit present; check if FAWE classes are also available
            try {
                Class.forName("com.fastasyncworldedit.core.Fawe");
                plugin.getLogger().info("Detected WorldEdit and FAWE on classpath.");
                return true;
            } catch (ClassNotFoundException ignored) {
                // Plain WorldEdit present but not FAWE
                plugin.getLogger().info("Detected WorldEdit on classpath (non-FAWE). Will use WorldEdit adapter if compatible.");
                return true; // treat plain WorldEdit as usable
            }
        } catch (ClassNotFoundException ignored) {
        }

        return false;
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
		maxRadius = configuredMaxRadius > 0.0D ? configuredMaxRadius : Double.POSITIVE_INFINITY;
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

		ConfigurationSection blockSection = orbSection.getConfigurationSection("block");
		String materialName = blockSection != null ? blockSection.getString("material", "SEA_LANTERN") : "SEA_LANTERN";
		Material parsedMaterial = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
		if (parsedMaterial == null || !parsedMaterial.isBlock()) {
			plugin.getLogger().warning("Invalid finale orb block material '" + materialName + "'. Falling back to SEA_LANTERN.");
			parsedMaterial = Material.SEA_LANTERN;
		}
		orbMaterial = parsedMaterial;
		blocksPerTick = Math.max(1, blockSection != null ? blockSection.getInt("blocks_per_tick", 800) : 800);
		int defaultBurst = Math.max(blocksPerTick, blocksPerTick * 3);
		initialBurstBlocks = Math.max(blocksPerTick, blockSection != null ? blockSection.getInt("initial_burst", defaultBurst) : defaultBurst);

		ConfigurationSection punishmentSection = orbSection.getConfigurationSection("punishment");
		banMessage = punishmentSection != null ? punishmentSection.getString("ban_message", "&cThe finale orb has consumed you.") : "&cThe finale orb has consumed you.";
		kickMessage = punishmentSection != null ? punishmentSection.getString("kick_message", "&cEven operators cannot outrun the finale orb.") : "&cEven operators cannot outrun the finale orb.";
		broadcastPunishment = punishmentSection != null && punishmentSection.getBoolean("broadcast", false);
		broadcastPunishmentMessage = punishmentSection != null ? punishmentSection.getString("broadcast_message", "&5[Halloween]&c %PLAYER% was consumed by the finale orb.") : "&5[Halloween]&c %PLAYER% was consumed by the finale orb.";
		bypassPermission = punishmentSection != null ? punishmentSection.getString("bypass_permission", "") : "";

		ConfigurationSection broadcastSection = orbSection.getConfigurationSection("broadcast");
		broadcastStartMessage = broadcastSection != null ? broadcastSection.getString("start_message", "&5[Halloween]&f The finale orb awakens at the origin.") : "&5[Halloween]&f The finale orb awakens at the origin.";
		broadcastStopMessage = broadcastSection != null ? broadcastSection.getString("stop_message", "&5[Halloween]&f The finale orb has faded.") : "&5[Halloween]&f The finale orb has faded.";

		synchronized (stateLock) {
			if (!enabled) {
				stopFinaleInternal();
			}
		}
	}

	public FinaleActionResult startFinale(String source) {
		synchronized (stateLock) {
			if (!enabled) {
				return FinaleActionResult.failure("&cFinale orb is disabled in halloween.yml.");
			}
			if (active) {
				return FinaleActionResult.failure("&eFinale orb is already running.");
			}

			List<World> targets = resolveWorlds();
			if (targets.isEmpty()) {
				return FinaleActionResult.failure("&cNo configured finale worlds are currently loaded.");
			}

			active = true;
			punishedPlayers.clear();
			punishedCount = 0;

			for (World world : targets) {
				OrbInstance instance = new OrbInstance(world);
				instance.start();
				activeOrbs.put(world.getUID(), instance);
			}

			if (broadcastStartMessage != null && !broadcastStartMessage.trim().isEmpty()) {
				Bukkit.broadcastMessage(MessagesManager.getColoredMessage(broadcastStartMessage));
			}

			plugin.getLogger().info("Halloween finale orb started by " + (source != null ? source : "unknown trigger") + ".");
			return FinaleActionResult.success("&aFinale orb awakened.");
		}
	}

	public FinaleActionResult stopFinale(String source) {
		synchronized (stateLock) {
			if (!active) {
				return FinaleActionResult.failure("&eFinale orb is not currently running.");
			}

			stopFinaleInternal();

			if (broadcastStopMessage != null && !broadcastStopMessage.trim().isEmpty()) {
				Bukkit.broadcastMessage(MessagesManager.getColoredMessage(broadcastStopMessage));
			}

			plugin.getLogger().info("Halloween finale orb stopped by " + (source != null ? source : "unknown trigger") + ".");
			return FinaleActionResult.success("&aFinale orb halted.");
		}
	}

	private void stopFinaleInternal() {
		for (OrbInstance instance : activeOrbs.values()) {
			instance.stop();
		}
		activeOrbs.clear();
		active = false;
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
				OrbInstance instance = world != null ? activeOrbs.get(world.getUID()) : null;
				boolean running = instance != null && instance.isRunning();
				double radius = running ? instance.getRadius() : 0.0D;
				String environment = world != null ? world.getEnvironment().name() : "UNLOADED";
				statuses.add(new OrbWorldStatus(name, environment, running, radius));
			}
			return new OrbStatus(
					enabled,
					active,
					startRadius,
					expansionPerTick,
					maxRadius,
					tickInterval,
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
		Location center = new Location(world, originX, y, originZ);
		world.getChunkAt(center).load();
		return center;
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
		String colored = MessagesManager.getColoredMessage(message != null ? message : "&cConsumed by the finale orb.");

		if (isOp) {
			player.kickPlayer(colored);
			plugin.getLogger().info("Finale orb kicked operator " + name + ".");
		} else {
			Bukkit.getBanList(BanList.Type.NAME).addBan(name, colored, null, "Halloween Finale Orb");
			player.kickPlayer(colored);
			plugin.getLogger().info("Finale orb banned player " + name + ".");
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
			if (!permission.isEmpty() && player.isPermissionSet(permission) && player.hasPermission(permission)) {
				return true;
			}
		}
		return false;
	}

	private void handleInstanceFinished(UUID worldId) {
		synchronized (stateLock) {
			activeOrbs.remove(worldId);
			if (activeOrbs.isEmpty()) {
				active = false;
				if (broadcastStopMessage != null && !broadcastStopMessage.trim().isEmpty()) {
					Bukkit.broadcastMessage(MessagesManager.getColoredMessage(broadcastStopMessage));
				}
			}
		}
	}

	private final class OrbInstance implements Runnable {
		private final World world;
		private final Location center;
		private final double centerX;
		private final double centerY;
		private final double centerZ;
		private final double maxRadiusSquared;
		private final double worldMaxRadius;
		private final int minY;
		private final int maxY;
		private final PriorityQueue<PendingBlock> candidates;
		private final Queue<Long> placementQueue;
		private final Set<Long> occupied;
		private final Set<Long> enqueued;
		private BukkitTask task;
		private double radius;
		private boolean running;

		private OrbInstance(World world) {
			this.world = Objects.requireNonNull(world, "world");
			this.center = computeCenter(world);
			this.centerX = center.getX();
			this.centerY = center.getY();
			this.centerZ = center.getZ();
			this.radius = Math.max(0.0D, startRadius);
			this.maxRadiusSquared = Double.isFinite(maxRadius) ? maxRadius * maxRadius : Double.POSITIVE_INFINITY;
			this.worldMaxRadius = maxRadius;
			this.minY = 0;
			this.maxY = world.getMaxHeight() - 1;
			this.candidates = new PriorityQueue<>(Comparator.comparingDouble(p -> p.distanceSquared));
			this.placementQueue = new ArrayDeque<>();
			this.occupied = new HashSet<>();
			this.enqueued = new HashSet<>();
		}

		private void start() {
			running = true;
			long centerPos = packBlockPosition(center.getBlockX(), center.getBlockY(), center.getBlockZ());
			occupied.add(centerPos);
			placementQueue.offer(centerPos);
			addNeighbors(centerPos);
			expandToRadius(radius);
			flushPlacementQueue(initialBurstBlocks);
			checkPlayers();
			task = plugin.getServer().getScheduler().runTaskTimer(plugin, this, tickInterval, tickInterval);
		}

		private void stop() {
			running = false;
			if (task != null) {
				task.cancel();
				task = null;
			}
		}

		@Override
		public void run() {
			if (!running) {
				return;
			}

			radius = Math.min(Double.isFinite(worldMaxRadius) ? worldMaxRadius : radius + expansionPerTick, radius + expansionPerTick);
			expandToRadius(radius);
			flushPlacementQueue(blocksPerTick);
			checkPlayers();

			if (Double.isFinite(worldMaxRadius)) {
				if (radius >= worldMaxRadius - STOP_EPSILON && candidates.isEmpty() && placementQueue.isEmpty()) {
					stop();
					handleInstanceFinished(world.getUID());
				}
			}
		}

		private void expandToRadius(double currentRadius) {
			double radiusSquared = currentRadius * currentRadius;
			while (!candidates.isEmpty() && candidates.peek().distanceSquared <= radiusSquared) {
				PendingBlock pending = candidates.poll();
				long pos = pending.packedPosition;
				if (occupied.contains(pos)) {
					enqueued.remove(pos);
					continue;
				}
				occupied.add(pos);
				enqueued.remove(pos);
				placementQueue.offer(pos);
				addNeighbors(pos);
			}
		}

		private void flushPlacementQueue(int limit) {
			int processed = 0;
			// Batch placements via adapter for performance; fall back to per-block Bukkit placement inside adapter
			List<voiidstudios.vct.integrations.worldedit.BlockPlacement> batch = new ArrayList<>();
			while (processed < limit && !placementQueue.isEmpty()) {
				long packed = placementQueue.poll();
				int blockX = unpackX(packed);
				int blockY = unpackY(packed);
				int blockZ = unpackZ(packed);
				int chunkX = blockX >> 4;
				int chunkZ = blockZ >> 4;
				if (!world.isChunkLoaded(chunkX, chunkZ)) {
					world.getChunkAt(chunkX, chunkZ).load(true);
				}
				batch.add(new voiidstudios.vct.integrations.worldedit.BlockPlacement(blockX, blockY, blockZ, orbMaterial));
				processed++;
			}

			if (!batch.isEmpty()) {
				try {
					blockPlacementAdapter.placeBlocks(world, batch);
				} catch (Throwable t) {
					plugin.getLogger().warning("Block placement adapter failed during finale; falling back to Bukkit per-block placement: " + t.getMessage());
					// fallback per-block
					for (voiidstudios.vct.integrations.worldedit.BlockPlacement p : batch) {
						Block b = world.getBlockAt(p.getX(), p.getY(), p.getZ());
						b.setType(p.getMaterial(), false);
					}
				}
			}
		}

		private void addNeighbors(long base) {
			int x = unpackX(base);
			int y = unpackY(base);
			int z = unpackZ(base);
			tryQueueNeighbor(x + 1, y, z);
			tryQueueNeighbor(x - 1, y, z);
			tryQueueNeighbor(x, y + 1, z);
			tryQueueNeighbor(x, y - 1, z);
			tryQueueNeighbor(x, y, z + 1);
			tryQueueNeighbor(x, y, z - 1);
		}

		private void tryQueueNeighbor(int x, int y, int z) {
			if (y < minY || y > maxY) {
				return;
			}
			long packed = packBlockPosition(x, y, z);
			if (occupied.contains(packed) || enqueued.contains(packed)) {
				return;
			}
			double distanceSquared = distanceSquared(x, y, z);
			if (distanceSquared > maxRadiusSquared) {
				return;
			}
			candidates.offer(new PendingBlock(packed, distanceSquared));
			enqueued.add(packed);
		}

		private double distanceSquared(int blockX, int blockY, int blockZ) {
			double dx = (blockX + 0.5D) - centerX;
			double dy = (blockY + 0.5D) - centerY;
			double dz = (blockZ + 0.5D) - centerZ;
			return dx * dx + dy * dy + dz * dz;
		}

		private void checkPlayers() {
			// Only punish players when they are actually covered by a placed orb block.
			for (Player player : world.getPlayers()) {
				if (!player.isOnline()) {
					continue;
				}
				if (shouldBypass(player)) {
					continue;
				}

				// Check the block at the player's feet and the block at head height.
				Location loc = player.getLocation();
				int bx = loc.getBlockX();
				int by = loc.getBlockY();
				int bz = loc.getBlockZ();

				Block foot = world.getBlockAt(bx, by, bz);
				boolean covered = foot != null && foot.getType() == orbMaterial;

				if (!covered) {
					int headY = by + 1;
					if (headY <= maxY) {
						Block head = world.getBlockAt(bx, headY, bz);
						covered = head != null && head.getType() == orbMaterial;
					}
				}

				if (covered) {
					registerPunishment(player);
				}
			}
		}

		private boolean isRunning() {
			return running;
		}

		private double getRadius() {
			return radius;
		}
	}

	private static long packBlockPosition(int x, int y, int z) {
		return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (long) (y & 0xFFFL);
	}

	private static int unpackX(long packed) {
		return (int) (packed >> 38);
	}

	private static int unpackY(long packed) {
		return (int) (packed << 52 >> 52);
	}

	private static int unpackZ(long packed) {
		return (int) (packed << 26 >> 38);
	}

	private static final class PendingBlock {
		private final long packedPosition;
		private final double distanceSquared;

		private PendingBlock(long packedPosition, double distanceSquared) {
			this.packedPosition = packedPosition;
			this.distanceSquared = distanceSquared;
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
		private final double startRadius;
		private final double expansionPerTick;
		private final double maxRadius;
		private final long tickInterval;
		private final List<OrbWorldStatus> worlds;
		private final int punishedCount;

		private OrbStatus(boolean enabled,
						  boolean active,
						  double startRadius,
						  double expansionPerTick,
						  double maxRadius,
						  long tickInterval,
						  List<OrbWorldStatus> worlds,
						  int punishedCount) {
			this.enabled = enabled;
			this.active = active;
			this.startRadius = startRadius;
			this.expansionPerTick = expansionPerTick;
			this.maxRadius = maxRadius;
			this.tickInterval = tickInterval;
			this.worlds = worlds;
			this.punishedCount = punishedCount;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public boolean isActive() {
			return active;
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
		private final double radius;

		private OrbWorldStatus(String worldName, String environment, boolean running, double radius) {
			this.worldName = worldName;
			this.environment = environment;
			this.running = running;
			this.radius = radius;
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

		public double getRadius() {
			return radius;
		}
	}
}
