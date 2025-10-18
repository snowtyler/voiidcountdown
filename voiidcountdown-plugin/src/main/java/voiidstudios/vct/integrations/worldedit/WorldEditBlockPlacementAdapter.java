package voiidstudios.vct.integrations.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter that batches block changes through the WorldEdit/FAWE API.
 */
public final class WorldEditBlockPlacementAdapter implements BlockPlacementAdapter {
    private final Logger logger;
    private final BlockPlacementAdapter fallback = new BukkitBlockPlacementAdapter();

    public WorldEditBlockPlacementAdapter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void placeBlocks(World world, List<BlockPlacement> placements) {
        if (world == null || placements == null || placements.isEmpty()) {
            return;
        }

        Map<Material, BlockState> cache = new HashMap<>();
        List<BlockPlacement> safePlacements = new ArrayList<>(placements.size());
        for (BlockPlacement placement : placements) {
            if (placement != null) {
                safePlacements.add(placement);
            }
        }

        if (safePlacements.isEmpty()) {
            return;
        }

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        try (EditSession editSession = WorldEdit.getInstance()
                .newEditSessionBuilder()
                .world(weWorld)
                .maxBlocks(-1)
                .build()) {
            for (BlockPlacement placement : safePlacements) {
                ensureChunkLoaded(world, placement);
                BlockVector3 position = BlockVector3.at(placement.getX(), placement.getY(), placement.getZ());
                BlockState state = cache.computeIfAbsent(placement.getMaterial(), material -> adapt(material));
                try {
                    editSession.setBlock(position, state);
                } catch (WorldEditException ex) {
                    if (logger != null) {
                        logger.log(Level.FINEST, "Failed to set block via WorldEdit, will retry with fallback.", ex);
                    }
                    fallback.placeBlocks(world, Collections.singletonList(placement));
                }
            }
        } catch (Exception ex) {
            if (logger != null) {
                logger.log(Level.WARNING, "Failed to apply WorldEdit placements; falling back to Bukkit placement for this batch.", ex);
            }
            // Fallback: perform placements using Bukkit directly to avoid losing changes.
            fallback.placeBlocks(world, safePlacements);
        }
    }

    private static void ensureChunkLoaded(World world, BlockPlacement placement) {
        int chunkX = placement.getX() >> 4;
        int chunkZ = placement.getZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            if (!chunk.isLoaded()) {
                chunk.load(true);
            }
        }
    }

    private static BlockState adapt(Material material) {
        Material target = (material == null || !material.isBlock()) ? Material.AIR : material;
        return BukkitAdapter.adapt(target.createBlockData());
    }
}
