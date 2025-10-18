package voiidstudios.vct.integrations.worldedit;

import org.bukkit.Material;

/**
 * Represents a single block placement operation.
 */
public final class BlockPlacement {
    private final int x;
    private final int y;
    private final int z;
    private final Material material;

    public BlockPlacement(int x, int y, int z, Material material) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.material = material;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Material getMaterial() {
        return material;
    }

    public BlockPlacement withMaterial(Material replacement) {
        if (replacement == material) {
            return this;
        }
        return new BlockPlacement(x, y, z, replacement);
    }
}
