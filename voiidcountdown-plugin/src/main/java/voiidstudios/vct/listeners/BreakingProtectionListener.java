package voiidstudios.vct.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.block.Block;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.managers.VisualBlockManager;

public class BreakingProtectionListener implements Listener {
    private final VisualBlockManager vbm;

    public BreakingProtectionListener(VisualBlockManager visualBlockManager) {
        this.vbm = visualBlockManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (vbm.isProtectedLocation(b.getLocation())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(voiidstudios.vct.managers.MessagesManager.getColoredMessage(VoiidCountdownTimer.prefix + "&cYou cannot break that test pillar block."));
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> vbm.isProtectedLocation(b.getLocation()));
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        e.getBlocks().removeIf(b -> vbm.isProtectedLocation(b.getLocation()));
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        e.getBlocks().removeIf(b -> vbm.isProtectedLocation(b.getLocation()));
    }
}
