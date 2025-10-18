package voiidstudios.vct.integrations.worldedit;

import org.bukkit.Material;
import org.bukkit.World;

import java.util.List;
import java.util.ArrayList;

/**
 * Abstraction over block placement so we can swap between Bukkit and WorldEdit/FAWE at runtime.
 */
public interface BlockPlacementAdapter {
    void placeBlocks(World world, List<BlockPlacement> placements);

    default void clearBlocks(World world, List<BlockPlacement> placements) {
        // default implementation replaces blocks with air
        if (placements == null || placements.isEmpty()) {
            return;
        }
        List<BlockPlacement> cleared = new ArrayList<>(placements.size());
        for (BlockPlacement placement : placements) {
            cleared.add(placement.withMaterial(Material.AIR));
        }
        placeBlocks(world, cleared);
    }

    default void shutdown() {
        // adapters can override when they need to close resources
    }
}
