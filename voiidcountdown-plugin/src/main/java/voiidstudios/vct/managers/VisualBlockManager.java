package voiidstudios.vct.managers;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BlockFace;
import org.bukkit.block.EndGateway;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.api.Timer;
import voiidstudios.vct.configs.MainConfigManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

/**
 * Manages real test pillars by placing configurable rings with a staged "fall" animation.
 */
public class VisualBlockManager {
    private final JavaPlugin plugin;
    private final Map<String, PillarSpec> pillars = new ConcurrentHashMap<>();

    private static String coordKey(int x, int y, int z) {
        return x + ":" + y + ":" + z;
    }

    private static final class BlockPosition {
        final int x;
        final int y;
        final int z;

        BlockPosition(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static class PillarSpec {
        public final UUID worldId;
        public final int x;
        public final int z;
        public final int minY;
        public final int maxY;
        public final boolean unbreakable;
        public final double radius;
        public final double thickness;
        private final Map<String, BlockState> originalStates = new HashMap<>();
        private final List<List<BlockPosition>> layers;
        private BukkitTask buildTask;
        private int nextLayerIndex = 0;

        public PillarSpec(UUID worldId, int x, int z, int minY, int maxY, boolean unbreakable, double radius, double thickness, List<List<BlockPosition>> layers) {
            this.worldId = worldId;
            this.x = x;
            this.z = z;
            this.minY = minY;
            this.maxY = maxY;
            this.unbreakable = unbreakable;
            this.radius = radius;
            this.thickness = thickness;
            this.layers = layers;
        }

        public String key() { return worldId.toString() + ":" + x + ":" + z; }

        public boolean contains(int wx, int wy, int wz, UUID wid) {
            if (!this.worldId.equals(wid)) return false;
            double dx = wx - x;
            double dz = wz - z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            double outer = radius + (thickness / 2.0);
            double inner = Math.max(0.0, radius - (thickness / 2.0));
            return dist >= inner && dist <= outer && wy >= minY && wy < maxY;
        }

        public void remember(BlockState state) {
            originalStates.putIfAbsent(coordKey(state.getX(), state.getY(), state.getZ()), state);
        }

        public Collection<BlockState> getOriginalStates() {
            return originalStates.values();
        }

        public boolean hasOriginalAt(int x, int y, int z) {
            return originalStates.containsKey(coordKey(x, y, z));
        }

        public List<List<BlockPosition>> getLayers() {
            return layers;
        }

        public void setTask(BukkitTask task) {
            this.buildTask = task;
        }

        public BukkitTask getTask() {
            return buildTask;
        }

        public int getNextLayerIndex() {
            return nextLayerIndex;
        }

        public void advanceLayers(int amount) {
            nextLayerIndex += amount;
        }

        public boolean isComplete() {
            return nextLayerIndex >= layers.size();
        }
    }

    public VisualBlockManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Collection<PillarSpec> getAllPillars() { return Collections.unmodifiableCollection(pillars.values()); }

    public Optional<PillarSpec> getPillar(UUID worldId, int x, int z) {
        return Optional.ofNullable(pillars.get(worldId.toString() + ":" + x + ":" + z));
    }

    public void addPillar(World world, int x, int z, int minY, int maxY) {
        removePillar(world, x, z);

        boolean unbreakable = true;
        double radius = 3.0;
        double thickness = 1.0;
        int layersPerTick = 4;
        double configuredChance = 0.15D;
        Material pillarMaterial = Material.END_GATEWAY;
        boolean startSoundEnabled = true;
        String startSoundName = null;
        float startSoundVolume = 1.0F;
        float startSoundPitch = 1.0F;
        try {
            MainConfigManager mainConfig = VoiidCountdownTimer.getConfigsManager().getMainConfigManager();
            unbreakable = mainConfig.isTestPillarUnbreakable();
            radius = mainConfig.getTestPillarRadius();
            thickness = mainConfig.getTestPillarThickness();
            layersPerTick = Math.max(1, mainConfig.getTestPillarLayersPerTick());
            configuredChance = mainConfig.getTestPillarGatewayChance();
            pillarMaterial = resolveMaterial(mainConfig.getTestPillarBlockType(), pillarMaterial);
            startSoundEnabled = mainConfig.isTestPillarStartSoundEnabled();
            startSoundName = mainConfig.getTestPillarStartSound();
            startSoundVolume = Math.max(0.0F, mainConfig.getTestPillarStartSoundVolume());
            startSoundPitch = mainConfig.getTestPillarStartSoundPitch();
        } catch (Throwable ignored) {}

        configuredChance = Math.max(0.0D, Math.min(1.0D, configuredChance));

        int maxRange = (int) Math.ceil(radius + thickness / 2.0);
        List<List<BlockPosition>> layers = new ArrayList<>();
        MainConfigManager mainConfigCheck = null;
        boolean startFromBottom = false;
        try {
            mainConfigCheck = VoiidCountdownTimer.getConfigsManager().getMainConfigManager();
            startFromBottom = mainConfigCheck.isTestPillarStartFromBottom();
        } catch (Throwable ignored) {}

        if (startFromBottom) {
            for (int y = minY; y < maxY; y++) {
                List<BlockPosition> layer = new ArrayList<>();
                for (int dx = -maxRange; dx <= maxRange; dx++) {
                    for (int dz = -maxRange; dz <= maxRange; dz++) {
                        int wx = x + dx;
                        int wz = z + dz;
                        double dxOffset = wx - x;
                        double dzOffset = wz - z;
                        double dist = Math.sqrt(dxOffset * dxOffset + dzOffset * dzOffset);
                        double outer = radius + (thickness / 2.0);
                        double inner = Math.max(0.0, radius - (thickness / 2.0));
                        if (dist >= inner && dist <= outer) {
                            layer.add(new BlockPosition(wx, y, wz));
                        }
                    }
                }
                if (!layer.isEmpty()) {
                    layers.add(layer);
                }
            }
        } else {
            for (int y = maxY - 1; y >= minY; y--) {
            List<BlockPosition> layer = new ArrayList<>();
                for (int dx = -maxRange; dx <= maxRange; dx++) {
                    for (int dz = -maxRange; dz <= maxRange; dz++) {
                        int wx = x + dx;
                        int wz = z + dz;
                        double dxOffset = wx - x;
                        double dzOffset = wz - z;
                        double dist = Math.sqrt(dxOffset * dxOffset + dzOffset * dzOffset);
                        double outer = radius + (thickness / 2.0);
                        double inner = Math.max(0.0, radius - (thickness / 2.0));
                        if (dist >= inner && dist <= outer) {
                            layer.add(new BlockPosition(wx, y, wz));
                        }
                    }
                }
                if (!layer.isEmpty()) {
                    layers.add(layer);
                }
            }
        }

        PillarSpec spec = new PillarSpec(world.getUID(), x, z, minY, maxY, unbreakable, radius, thickness, layers);
        if (layers.isEmpty()) {
            return;
        }

        pillars.put(spec.key(), spec);

        if (startSoundEnabled && startSoundName != null && !startSoundName.trim().isEmpty()) {
            int anchorY = startFromBottom ? minY : Math.max(minY, maxY - 1);
            playStartSound(world, x, anchorY, z, startSoundName, startSoundVolume, startSoundPitch);
        }

        final int finalLayersPerTick = layersPerTick;
        final Material finalPillarMaterial = pillarMaterial;
        final boolean mixWithBedrock = finalPillarMaterial == Material.END_GATEWAY;
        final Material fallbackMaterial = mixWithBedrock ? Material.BEDROCK : finalPillarMaterial;
        final double finalMixChance = mixWithBedrock ? configuredChance : 0.0D;
        final double finalSpecialChance = finalPillarMaterial == Material.END_PORTAL_FRAME ? configuredChance : 0.0D;
        final Random rng = new Random(
                world.getUID().getLeastSignificantBits()
                        ^ (((long) x) << 32)
                        ^ (z & 0xffffffffL)
                        ^ (((long) finalPillarMaterial.ordinal()) << 48)
        );
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (spec.isComplete()) {
                BukkitTask running = spec.getTask();
                if (running != null) running.cancel();
                return;
            }

            int applied = 0;
            while (applied < finalLayersPerTick && !spec.isComplete()) {
                int index = spec.getNextLayerIndex();
                List<BlockPosition> layer = spec.getLayers().get(index);
                if (mixWithBedrock) {
                    boolean primaryPlaced = false;
                    BlockPosition fallback = null;
                    for (BlockPosition pos : layer) {
                        if (fallback == null) fallback = pos;
                        boolean placePrimary = finalMixChance > 0.0D && rng.nextDouble() < finalMixChance;
                        if (placePrimary) {
                            primaryPlaced = true;
                        }
                        placeBlock(world, spec, pos, placePrimary ? finalPillarMaterial : fallbackMaterial, finalSpecialChance, rng);
                    }
                    if (!primaryPlaced && fallback != null) {
                        placeBlock(world, spec, fallback, finalPillarMaterial, finalSpecialChance, rng);
                    }
                } else {
                    for (BlockPosition pos : layer) {
                        placeBlock(world, spec, pos, finalPillarMaterial, finalSpecialChance, rng);
                    }
                }
                spec.advanceLayers(1);
                applied++;
            }

            if (spec.isComplete()) {
                BukkitTask running = spec.getTask();
                if (running != null) running.cancel();
                spec.setTask(null);
            }
        }, 0L, 4L);

        spec.setTask(task);
    }

    public boolean isProtectedLocation(Location loc) {
        for (PillarSpec spec : pillars.values()) {
            if (!spec.unbreakable) continue;
            if (spec.hasOriginalAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getKeys() { return pillars.keySet(); }

    public boolean removePillar(World world, int x, int z) {
        PillarSpec removed = pillars.remove(world.getUID().toString() + ":" + x + ":" + z);
        if (removed == null) return false;

        BukkitTask task = removed.getTask();
        if (task != null) {
            task.cancel();
        }

        for (BlockState original : removed.getOriginalStates()) {
            original.getWorld().getChunkAt(original.getX() >> 4, original.getZ() >> 4).load();
            original.update(true, false);
        }
        return true;
    }

    public void restoreAll() {
        for (String key : new ArrayList<>(pillars.keySet())) {
            String[] parts = key.split(":");
            if (parts.length != 3) continue;
            UUID worldId = UUID.fromString(parts[0]);
            World world = plugin.getServer().getWorld(worldId);
            if (world == null) continue;
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            removePillar(world, x, z);
        }
    }

    private void placeBlock(World world, PillarSpec spec, BlockPosition pos, Material material, double specialChance, Random rng) {
        world.getChunkAt(pos.x >> 4, pos.z >> 4).load();
        Block block = world.getBlockAt(pos.x, pos.y, pos.z);
        spec.remember(block.getState());

        if (block.getType() != material) {
            block.setType(material, false);
        }

        if (material == Material.END_GATEWAY) {
            BlockState state = block.getState();
            if (state instanceof EndGateway) {
                ((EndGateway) state).setAge(180L);
                state.update(true, false);
            }
        } else if (material == Material.END_PORTAL_FRAME) {
            BlockData data = block.getBlockData();
            if (data instanceof Directional) {
                ((Directional) data).setFacing(getFacingForPosition(spec, pos));
            }
            if (data instanceof EndPortalFrame) {
                boolean addEye = specialChance > 0.0D && rng.nextDouble() < specialChance;
                ((EndPortalFrame) data).setEye(addEye);
            }
            block.setBlockData(data, false);
        }
    }

    private BlockFace getFacingForPosition(PillarSpec spec, BlockPosition pos) {
        double dx = (pos.x + 0.5) - spec.x;
        double dz = (pos.z + 0.5) - spec.z;
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? BlockFace.EAST : BlockFace.WEST;
        }
        return dz >= 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }

    private void playStartSound(World world, int x, int y, int z, String soundName, float volume, float pitch) {
        if (world == null) {
            return;
        }

        String trimmedName = soundName != null ? soundName.trim() : "";
        if (trimmedName.isEmpty()) {
            return;
        }

        double safeVolume = Math.max(0.0D, volume);
        float useVolume = (float) Math.min(safeVolume, Float.MAX_VALUE);
        float usePitch = pitch;

        // Play the configured sound individually for every player in the same world so
        // each player hears it regardless of server-side positional issues or cross-world listeners.
        String actionLine = trimmedName + ";" + useVolume + ";" + usePitch;
        try {
            for (Player p : world.getPlayers()) {
                Timer.playSound(p, actionLine);
            }
        } catch (Throwable t) {
            // Fallback to logging the failure
            plugin.getLogger().warning("Unable to play configured test pillar start sound to players: " + trimmedName);
        }
    }

    private Material resolveMaterial(String name, Material fallback) {
        if (name == null || name.isEmpty()) {
            return fallback;
        }
        Material found = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        return found != null ? found : fallback;
    }

    // Legacy hooks retained for listeners that expect reapplication behaviour; now no-ops.
    public void reapplyToPlayer(Player player) {
        // No-op: real blocks persist naturally.
    }

    public void reapplyToChunk(Chunk chunk) {
        // No-op: real blocks persist naturally.
    }
}
