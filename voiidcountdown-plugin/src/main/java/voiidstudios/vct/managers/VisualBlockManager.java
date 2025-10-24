package voiidstudios.vct.managers;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.EndGateway;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Drives the animated construction of the configurable test pillar.
 */
public class VisualBlockManager {
    private static final double DISTANCE_EPSILON = 1.0E-6D;

    private final JavaPlugin plugin;
    private final Map<UUID, PillarJob> activePillars = new ConcurrentHashMap<>();

    public VisualBlockManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Legacy hook kept for compatibility; FAWE-driven placements are fully synchronous so there is nothing to replay.
     */
    public void reapplyToPlayer(Player player) {
        // No-op: blocks already exist in the world for every viewer.
    }

    /**
     * Legacy hook kept for compatibility; FAWE-driven placements are fully synchronous so there is nothing to replay.
     */
    public void reapplyToChunk(Chunk chunk) {
        // No-op: chunk contents already reflect the latest pillar state.
    }

    public void startTestPillar(World world,
                                int originX,
                                int originZ,
                                double radius,
                                Material initialMaterial,
                                Material finalMaterial,
                                int layersPerTick,
                                int replacementDelayTicks,
                                int tickIntervalTicks,
                                boolean unbreakable) {
        if (world == null) {
            return;
        }

        PillarJob existing = activePillars.remove(world.getUID());
        if (existing != null) {
            existing.restoreOriginals();
        }

        Material useInitial = initialMaterial != null ? initialMaterial : Material.END_GATEWAY;
        Material useFinal = finalMaterial != null ? finalMaterial : Material.BEDROCK;
        int safeLayersPerTick = Math.max(1, layersPerTick);
        int safeReplacementDelay = Math.max(0, replacementDelayTicks);
        double safeRadius = Math.max(0.0D, radius);

        int minY = resolveWorldMinY(world);
        int maxY = world.getMaxHeight();
        List<List<BlockPosition>> layers = buildLayers(originX, originZ, safeRadius, minY, maxY);
        if (layers.isEmpty()) {
            return;
        }

        int safeTickInterval = Math.max(1, tickIntervalTicks);

        PillarJob job = new PillarJob(world, useInitial, useFinal, safeLayersPerTick, safeReplacementDelay, safeTickInterval, unbreakable, layers);
        activePillars.put(world.getUID(), job);
        job.start();
    }

    public boolean removePillar(World world) {
        if (world == null) {
            return false;
        }
        PillarJob job = activePillars.remove(world.getUID());
        if (job == null) {
            return false;
        }
        job.restoreOriginals();
        return true;
    }

    public void restoreAll() {
        List<PillarJob> jobs = new ArrayList<>(activePillars.values());
        activePillars.clear();
        for (PillarJob job : jobs) {
            job.restoreOriginals();
        }
    }

    public boolean isProtectedLocation(Location location) {
        if (location == null) {
            return false;
        }
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        PillarJob job = activePillars.get(world.getUID());
        if (job == null) {
            return false;
        }
        return job.protects(location);
    }

    private int resolveWorldMinY(World world) {
        try {
            Method method = world.getClass().getMethod("getMinHeight");
            Object value = method.invoke(world);
            if (value instanceof Integer) {
                return (Integer) value;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return 0;
    }

    private List<List<BlockPosition>> buildLayers(int originX, int originZ, double radius, int minY, int maxY) {
        List<List<BlockPosition>> layers = new ArrayList<>();
        double outerRadius = Math.max(0.0D, radius);
        double innerRadius = Math.max(0.0D, outerRadius - 1.0D);
        boolean solidColumn = outerRadius < 1.0D;
        double outerRadiusSquared = outerRadius * outerRadius;
        double innerRadiusSquared = innerRadius * innerRadius;
        int horizontalRadius = (int) Math.ceil(Math.max(outerRadius, 0.5D));

        for (int y = minY; y < maxY; y++) {
            List<BlockPosition> layer = new ArrayList<>();
            for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
                for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                    int x = originX + dx;
                    int z = originZ + dz;

                    if (solidColumn) {
                        if (dx == 0 && dz == 0) {
                            layer.add(new BlockPosition(x, y, z));
                        }
                        continue;
                    }

                    if (dx == 0 && dz == 0) {
                        continue;
                    }

                    double distanceSq = (dx * dx) + (dz * dz);
                    if (distanceSq > outerRadiusSquared + DISTANCE_EPSILON) {
                        continue;
                    }
                    if (distanceSq + DISTANCE_EPSILON < innerRadiusSquared) {
                        continue;
                    }
                    layer.add(new BlockPosition(x, y, z));
                }
            }

            if (layer.isEmpty()) {
                layer.add(new BlockPosition(originX, y, originZ));
            }
            layers.add(layer);
        }

        return layers;
    }

    private static String coordKey(int x, int y, int z) {
        return x + ":" + y + ":" + z;
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private static int chunkX(long key) {
        return (int) (key >> 32);
    }

    private static int chunkZ(long key) {
        return (int) key;
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

    private final class PillarJob implements Runnable {
        private final World world;
        private final UUID worldId;
        private final Material initialMaterial;
        private final Material finalMaterial;
        private final int layersPerTick;
        private final int replacementDelayTicks;
        private final int tickIntervalTicks;
        private final boolean unbreakable;
        private final List<List<BlockPosition>> layers;
        private final Map<String, BlockState> originalStates = new HashMap<>();
        private final Set<String> touchedBlocks = new HashSet<>();
        private final Set<Long> affectedChunks = new HashSet<>();
        private final Set<Long> requestedChunkLoads = new HashSet<>();
        private final com.sk89q.worldedit.world.World weWorld;
        private final com.sk89q.worldedit.world.block.BlockState initialFaweState;
        private final com.sk89q.worldedit.world.block.BlockState finalFaweState;

        private BukkitTask task;
        private int buildIndex = 0;
        private int replaceIndex = 0;
        private long ticksElapsed = 0L;

        PillarJob(World world,
              Material initialMaterial,
                  Material finalMaterial,
                  int layersPerTick,
                  int replacementDelayTicks,
                  int tickIntervalTicks,
                  boolean unbreakable,
                  List<List<BlockPosition>> layers) {
            this.world = world;
            this.worldId = world.getUID();
            this.initialMaterial = initialMaterial;
            this.finalMaterial = finalMaterial;
            this.layersPerTick = Math.max(1, layersPerTick);
            this.replacementDelayTicks = Math.max(0, replacementDelayTicks);
            this.tickIntervalTicks = Math.max(1, tickIntervalTicks);
            this.unbreakable = unbreakable;
            this.layers = layers;
            this.weWorld = BukkitAdapter.adapt(world);

            com.sk89q.worldedit.world.block.BlockState resolvedInitial;
            com.sk89q.worldedit.world.block.BlockState resolvedFinal;
            try {
                resolvedInitial = BukkitAdapter.adapt(initialMaterial.createBlockData());
            } catch (IllegalArgumentException ex) {
                VisualBlockManager.this.plugin.getLogger().log(Level.WARNING,
                        "Unable to adapt initial material " + initialMaterial + ", defaulting to END_GATEWAY", ex);
                resolvedInitial = BukkitAdapter.adapt(Material.END_GATEWAY.createBlockData());
            }
            try {
                resolvedFinal = BukkitAdapter.adapt(finalMaterial.createBlockData());
            } catch (IllegalArgumentException ex) {
                VisualBlockManager.this.plugin.getLogger().log(Level.WARNING,
                        "Unable to adapt final material " + finalMaterial + ", defaulting to BEDROCK", ex);
                resolvedFinal = BukkitAdapter.adapt(Material.BEDROCK.createBlockData());
            }
            this.initialFaweState = resolvedInitial;
            this.finalFaweState = resolvedFinal;

            for (List<BlockPosition> layer : layers) {
                for (BlockPosition position : layer) {
                    affectedChunks.add(chunkKey(position.x >> 4, position.z >> 4));
                }
            }
        }

        void start() {
            preloadChunks();
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this, 0L, tickIntervalTicks);
        }

        @Override
        public void run() {
            ticksElapsed += tickIntervalTicks;

            int buildBudget = layersPerTick;
            boolean layerBlocked = false;
            while (buildBudget > 0 && buildIndex < layers.size()) {
                if (!placeLayer(initialMaterial, buildIndex)) {
                    layerBlocked = true;
                    break;
                }
                buildIndex++;
                buildBudget--;
            }

            if (!layerBlocked && ticksElapsed >= replacementDelayTicks) {
                int replaceBudget = layersPerTick;
                while (replaceBudget > 0 && replaceIndex < buildIndex) {
                    if (!placeLayer(finalMaterial, replaceIndex)) {
                        layerBlocked = true;
                        break;
                    }
                    replaceIndex++;
                    replaceBudget--;
                }
            }

            if (!layerBlocked && buildIndex >= layers.size() && replaceIndex >= buildIndex) {
                cancel();
            }
        }

        private void preloadChunks() {
            for (long key : affectedChunks) {
                int chunkX = chunkX(key);
                int chunkZ = chunkZ(key);
                if (world.isChunkLoaded(chunkX, chunkZ)) {
                    continue;
                }
                requestChunk(chunkX, chunkZ);
            }
        }

        private boolean placeLayer(Material material, int layerIndex) {
            List<BlockPosition> layer = layers.get(layerIndex);
            for (BlockPosition position : layer) {
                int chunkX = position.x >> 4;
                int chunkZ = position.z >> 4;
                if (!ensureChunkReady(chunkX, chunkZ)) {
                    return false;
                }
            }
            com.sk89q.worldedit.world.block.BlockState targetState = (material == initialMaterial)
                    ? initialFaweState
                    : finalFaweState;

                    try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                        .world(weWorld)
                        .fastMode(true)
                        .allowedRegionsEverywhere()
                        .build()) {
                for (BlockPosition position : layer) {
                    Block block = world.getBlockAt(position.x, position.y, position.z);
                    String key = coordKey(position.x, position.y, position.z);
                    originalStates.computeIfAbsent(key, k -> block.getState());
                    touchedBlocks.add(key);
                    editSession.setBlock(position.x, position.y, position.z, targetState);
                }
                editSession.flushQueue();
            } catch (WorldEditException e) {
                VisualBlockManager.this.plugin.getLogger().log(Level.WARNING,
                        "Failed to queue pillar layer in world " + world.getName(), e);
                return false;
            }

            if (material == Material.END_GATEWAY) {
                for (BlockPosition position : layer) {
                    BlockState state = world.getBlockAt(position.x, position.y, position.z).getState();
                    if (state instanceof EndGateway) {
                        ((EndGateway) state).setAge(400L);
                        state.update(true, false);
                    }
                }
            }
            return true;
        }

        void cancel() {
            if (task != null) {
                task.cancel();
                task = null;
            }
        }

        void restoreOriginals() {
            cancel();
            Set<Long> restoreChunks = new HashSet<>();
            for (BlockState state : originalStates.values()) {
                int chunkX = state.getX() >> 4;
                int chunkZ = state.getZ() >> 4;
                if (!state.getWorld().isChunkLoaded(chunkX, chunkZ)) {
                    long key = chunkKey(chunkX, chunkZ);
                    if (restoreChunks.add(key)) {
                        requestChunk(chunkX, chunkZ);
                    }
                    if (!state.getWorld().isChunkLoaded(chunkX, chunkZ)) {
                        continue;
                    }
                }
                state.update(true, false);
            }
        }

        boolean protects(Location location) {
            if (!unbreakable) {
                return false;
            }
            if (!worldId.equals(location.getWorld().getUID())) {
                return false;
            }
            return touchedBlocks.contains(coordKey(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
        }

        private boolean ensureChunkReady(int chunkX, int chunkZ) {
            if (world.isChunkLoaded(chunkX, chunkZ)) {
                return true;
            }
            requestChunk(chunkX, chunkZ);
            return world.isChunkLoaded(chunkX, chunkZ);
        }

        private void requestChunk(int chunkX, int chunkZ) {
            long key = chunkKey(chunkX, chunkZ);
            if (!requestedChunkLoads.add(key)) {
                return;
            }
            try {
                world.loadChunk(chunkX, chunkZ, false);
            } catch (NoSuchMethodError | AbstractMethodError ignored) {
                world.loadChunk(chunkX, chunkZ);
            }
        }
    }
}
