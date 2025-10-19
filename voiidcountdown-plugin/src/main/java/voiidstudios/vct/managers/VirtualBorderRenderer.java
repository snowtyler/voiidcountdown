package voiidstudios.vct.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Renders a client-side "virtual" border by sending BLOCK_CHANGE packets via ProtocolLib.
 * Only visual: players can still walk through the blocks, so server-side punishment must
 * be handled separately by the caller.
 */
public class VirtualBorderRenderer {
    private final Plugin plugin;
    private final ProtocolManager protocolManager;
    private final World world;
    private final double centerX;
    private final double centerZ;
    private final int height;
    private final int density;
    private final WrappedBlockData visualData;

    private final Map<UUID, Set<Long>> shownBlocks = new ConcurrentHashMap<>();
    private volatile Set<Long> currentShell = Collections.emptySet();
    private volatile double currentRadius = 0.0D;

    public VirtualBorderRenderer(Plugin plugin,
                                 World world,
                                 double centerX,
                                 double centerZ,
                                 int height,
                                 int density,
                                 Material visualMaterial) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.world = world;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.height = Math.max(1, height);
        this.density = Math.max(1, density);
        BlockData blockData = (visualMaterial != null ? visualMaterial : Material.RED_STAINED_GLASS).createBlockData();
        this.visualData = WrappedBlockData.createData(blockData);
    }

    /**
        * Render a hollow vertical ring (cylindrical shell) at the supplied radius.
        * This ring extends from the bottom to the top of the world and is sampled
        * around the XZ circle at each vertical column using the configured height.
        * Radius is measured in blocks (radius, not diameter).
     */
    public void render(double radius) {
        if (radius < 0.5D) {
            clearAll();
            return;
        }

        Set<Long> shellPositions = computeCylindricalRingPositions(radius);
        currentShell = shellPositions;
        currentRadius = radius;

        for (Player player : world.getPlayers()) {
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (player.getWorld() != world) {
                clearForPlayer(player.getUniqueId());
                continue;
            }

            double dx = player.getLocation().getX() - centerX;
            double dz = player.getLocation().getZ() - centerZ;
            double distanceSquared = dx * dx + dz * dz;
            double maxVisible = Math.max(64.0D, radius + 32.0D);
            if (distanceSquared > maxVisible * maxVisible) {
                clearForPlayer(player.getUniqueId());
                continue;
            }

            Set<Long> previouslyShown = shownBlocks.computeIfAbsent(player.getUniqueId(), ignored -> Collections.synchronizedSet(new LinkedHashSet<>()));
            Set<Long> newShown = Collections.synchronizedSet(new LinkedHashSet<>());

            // Show new blocks
            for (Long packed : shellPositions) {
                if (!previouslyShown.contains(packed)) {
                    sendBlockChange(player, packed, visualData);
                }
                newShown.add(packed);
            }

            // Revert those no longer needed
            Set<Long> toRevert = new HashSet<>(previouslyShown);
            toRevert.removeAll(newShown);
            for (Long packed : toRevert) {
                revertBlock(player, packed);
            }

            previouslyShown.clear();
            previouslyShown.addAll(newShown);
        }
    }

    /**
     * Clears all client visuals and forgets state.
     */
    public void clearAll() {
        for (UUID uuid : new ArrayList<>(shownBlocks.keySet())) {
            clearForPlayer(uuid);
        }
        currentShell = Collections.emptySet();
        currentRadius = 0.0D;
    }

    private void clearForPlayer(UUID uuid) {
        Set<Long> blocks = shownBlocks.remove(uuid);
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return;
        }
        for (Long packed : blocks) {
            revertBlock(player, packed);
        }
    }

    private void revertBlock(Player player, long packed) {
        BlockData realData = getRealBlockData(packed);
        WrappedBlockData wrapped = WrappedBlockData.createData(realData);
        sendBlockChange(player, packed, wrapped);
    }

    private BlockData getRealBlockData(long packed) {
        int x = unpackX(packed);
        int y = unpackY(packed);
        int z = unpackZ(packed);
        Chunk chunk = world.getChunkAt(x >> 4, z >> 4);
        if (!chunk.isLoaded()) {
            // Avoid forcing chunk loads; fall back to air when chunk is absent
            return Material.AIR.createBlockData();
        }
        return world.getBlockAt(x, y, z).getBlockData();
    }

    private void sendBlockChange(Player player, long packed, WrappedBlockData data) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
        packet.getBlockPositionModifier().write(0, new BlockPosition(unpackX(packed), unpackY(packed), unpackZ(packed)));
        packet.getBlockData().write(0, data);
        try {
            protocolManager.sendServerPacket(player, packet);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to send virtual border packet to " + player.getName(), ex);
        }
    }

    private Set<Long> computeCylindricalRingPositions(double radius) {
        // Horizontal sampling around the XZ circle
        int horizontalSamples = Math.max(12, (int) Math.round(2.0D * Math.PI * radius / density));

        // We will produce vertical columns that span the entire world height.
        // Use the server-provided min/max heights implicitly by sampling a centered column
        // and extending it vertically via addColumn. The addColumn method will use
        // the configured 'height' as a vertical thickness around each sampled Y.

        Set<Long> points = new LinkedHashSet<>();

        // Choose a representative Y level: centerY rounded to int. We'll build columns
        // around this Y which will be offset by addColumn using 'height'. To make the
        // column extend to world top/bottom, call addColumn for every Y from 0 to maxHeight
        // would be expensive; instead, implement columns that cover the full vertical by
        // using a special addFullColumn call below.

        for (int h = 0; h < horizontalSamples; h++) {
            double theta = (2.0D * Math.PI * h) / horizontalSamples;
            double x = centerX + radius * Math.cos(theta);
            double z = centerZ + radius * Math.sin(theta);
            int ix = (int) Math.round(x);
            int iz = (int) Math.round(z);
            addFullColumn(points, ix, iz);
        }

        return points;
    }

    // Add a vertical column that spans from world bottom to world top at (x, z).
    // We sample blocks every 'height' interval to avoid sending every single block
    // when height is large. That keeps the visual dense while bounding packet counts.
    private void addFullColumn(Set<Long> points, int x, int z) {
        int minY = 0;
        int maxY = world.getMaxHeight() - 1;

        // We'll step by 'height' so each column is built from stacked chunks of the
        // configured visual 'height'. This maintains the intended vertical thickness
        // while ensuring the column reaches top/bottom.
        for (int y = minY; y <= maxY; y += Math.max(1, height)) {
            int centerYBlock = y + (height / 2);
            // ensure centerYBlock stays within bounds
            if (centerYBlock < minY) centerYBlock = minY;
            if (centerYBlock > maxY) centerYBlock = maxY;
            addColumn(points, x, centerYBlock, z);
        }
    }

    private void addColumn(Set<Long> points, int x, int y, int z) {
        int offsetStart = -(height - 1) / 2;
        for (int dy = 0; dy < height; dy++) {
            int by = y + offsetStart + dy;
            points.add(packPosition(x, by, z));
        }
    }

    private long packPosition(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (long) (y & 0xFFFL);
    }

    private int unpackX(long packed) {
        return (int) (packed >> 38);
    }

    private int unpackY(long packed) {
        return (int) (packed << 52 >> 52);
    }

    private int unpackZ(long packed) {
        return (int) (packed << 26 >> 38);
    }

    public double getCurrentRadius() {
        return currentRadius;
    }

    public void syncPlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (player.getWorld() != world) {
            return;
        }

        Set<Long> shellSnapshot = currentShell;
        if (shellSnapshot == null || shellSnapshot.isEmpty()) {
            return;
        }

        Set<Long> previouslyShown = shownBlocks.computeIfAbsent(player.getUniqueId(), ignored -> Collections.synchronizedSet(new LinkedHashSet<>()));
        Set<Long> newShown = Collections.synchronizedSet(new LinkedHashSet<>(shellSnapshot));

        for (Long packed : shellSnapshot) {
            if (!previouslyShown.contains(packed)) {
                sendBlockChange(player, packed, visualData);
            }
        }

        Set<Long> toRevert = new HashSet<>(previouslyShown);
        toRevert.removeAll(newShown);
        for (Long packed : toRevert) {
            revertBlock(player, packed);
        }

        previouslyShown.clear();
        previouslyShown.addAll(newShown);
    }

    public void forgetPlayer(UUID uuid) {
        clearForPlayer(uuid);
    }
}
