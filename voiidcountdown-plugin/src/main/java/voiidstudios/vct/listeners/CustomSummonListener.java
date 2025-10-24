package voiidstudios.vct.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.managers.MessagesManager;

/**
 * Detects custom block patterns and spawns special entities when completed.
 */
public class CustomSummonListener implements Listener {

    private static final Material SUMMON_BLOCK = Material.EMERALD_BLOCK;
    private static final String WITHER_TAG = "DarkWither";

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        // Only react to Wither skeleton skull placement
        if (!isWitherSkull(placed)) return;

        // The skull sits above some soul sand block; candidate centers are that block and its 4 cardinal neighbors
        Block base = placed.getRelative(BlockFace.DOWN);
        List<Block> candidates = new ArrayList<>();
        candidates.add(base);
        candidates.add(base.getRelative(BlockFace.NORTH));
        candidates.add(base.getRelative(BlockFace.SOUTH));
        candidates.add(base.getRelative(BlockFace.EAST));
        candidates.add(base.getRelative(BlockFace.WEST));

        Player player = event.getPlayer();

        for (Block center : candidates) {
            if (center == null || center.getType() != SUMMON_BLOCK) continue;
            Block stem = center.getRelative(BlockFace.DOWN);
            if (stem == null || stem.getType() != SUMMON_BLOCK) continue;

            // Check east-west arms
            Block e = center.getRelative(BlockFace.EAST);
            Block w = center.getRelative(BlockFace.WEST);
            boolean ew = e != null && w != null && e.getType() == SUMMON_BLOCK && w.getType() == SUMMON_BLOCK;

            // Check north-south arms
            Block n = center.getRelative(BlockFace.NORTH);
            Block s = center.getRelative(BlockFace.SOUTH);
            boolean ns = n != null && s != null && n.getType() == SUMMON_BLOCK && s.getType() == SUMMON_BLOCK;

            if (!ew && !ns) continue;

            // Determine skull positions based on orientation
            List<Block> patternBlocks = new ArrayList<>();
            patternBlocks.add(center);
            patternBlocks.add(stem);
            List<Block> skullBlocks = new ArrayList<>();
            Block skullCenter = center.getRelative(BlockFace.UP);
            // Require the center skull to be present for the ritual to be valid
            if (!isWitherSkull(skullCenter)) continue;
            skullBlocks.add(skullCenter);

            if (ew) {
                Block skullE = e != null ? e.getRelative(BlockFace.UP) : null;
                Block skullW = w != null ? w.getRelative(BlockFace.UP) : null;
                if (!isWitherSkull(skullE) || !isWitherSkull(skullW)) continue;
                skullBlocks.add(skullE);
                skullBlocks.add(skullW);
                patternBlocks.add(e);
                patternBlocks.add(w);
            } else {
                Block skullN = n != null ? n.getRelative(BlockFace.UP) : null;
                Block skullS = s != null ? s.getRelative(BlockFace.UP) : null;
                if (!isWitherSkull(skullN) || !isWitherSkull(skullS)) continue;
                skullBlocks.add(skullN);
                skullBlocks.add(skullS);
                patternBlocks.add(n);
                patternBlocks.add(s);
            }

            // Found a complete ritual — perform removal and spawn
            Location spawnLocation = center.getLocation().add(0.5, 0, 0.5);
            World world = center.getWorld();
            if (world == null) return;

            // Remove soul sand parts
            for (Block b : patternBlocks) {
                if (b != null) b.setType(Material.AIR, false);
            }
            // Remove skulls
            for (Block skull : skullBlocks) {
                if (skull != null) skull.setType(Material.AIR, false);
            }

            // Spawn the Wither and tag it
            Location witherSpawn = spawnLocation.clone().add(0.0, 1.0, 0.0);
            org.bukkit.entity.Wither wither = (org.bukkit.entity.Wither) world.spawnEntity(witherSpawn, EntityType.WITHER);
            if (wither != null) {
                wither.addScoreboardTag(WITHER_TAG);
                wither.setRemoveWhenFarAway(false);
                wither.setCustomName("DARK WITHER");
                wither.setCustomNameVisible(false);
            }

            if (player != null) {
                player.sendMessage(MessagesManager.getColoredMessage(VoiidCountdownTimer.prefix + "&cA Dark Wither has been summoned!"));
            }

            return; // only trigger once
        }
    }

    private boolean isWitherSkull(Block b) {
        if (b == null) return false;
        Material t = b.getType();
        return t == Material.WITHER_SKELETON_SKULL;
    }
}
