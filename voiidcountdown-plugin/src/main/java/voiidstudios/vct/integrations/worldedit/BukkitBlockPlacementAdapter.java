package voiidstudios.vct.integrations.worldedit;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;

/**
 * Simple adapter that sets blocks directly using the Bukkit API.
 */
public final class BukkitBlockPlacementAdapter implements BlockPlacementAdapter {
    @Override
    public void placeBlocks(World world, List<BlockPlacement> placements) {
        if (world == null || placements == null || placements.isEmpty()) {
            return;
        }

        for (BlockPlacement placement : placements) {
            if (placement == null) {
                continue;
            }

            int blockX = placement.getX();
            int blockY = placement.getY();
            int blockZ = placement.getZ();

            int chunkX = blockX >> 4;
            int chunkZ = blockZ >> 4;
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                if (!chunk.isLoaded()) {
                    chunk.load(true);
                }
            }

            Block block = world.getBlockAt(blockX, blockY, blockZ);
            Material material = placement.getMaterial();
            if (material != null) {
                block.setType(material, false);
            }
        }
    }
}
