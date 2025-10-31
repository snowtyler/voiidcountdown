package voiidstudios.vct.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.managers.LecternProtectionManager;

public class LecternProtectionListener implements Listener {
    public LecternProtectionListener() {}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Block b = event.getBlock();
		if (b == null || b.getType() != Material.LECTERN) return;
		LecternProtectionManager mgr = VoiidCountdownTimer.getLecternProtectionManager();
		if (mgr != null && mgr.isProtected(b.getWorld().getName(), b.getX(), b.getY(), b.getZ())) {
			event.setCancelled(true);
			if (event.getPlayer() != null) {
				// Reuse the configurable lectern 'alreadyProtected' message for block-break feedback
				VoiidCountdownTimer.getMessagesManager().sendConfigMessage(event.getPlayer(), "Messages.lectern.alreadyProtected", true, null);
			}
		}
	}

	// Optional: prevent removing the book via right-click swap on older servers where dedicated event may not fire
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event) {
		if (event.getHand() == EquipmentSlot.OFF_HAND) return; // allow main-hand only
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Block b = event.getClickedBlock();
		if (b == null || b.getType() != Material.LECTERN) return;
		LecternProtectionManager mgr = VoiidCountdownTimer.getLecternProtectionManager();
		if (mgr != null && mgr.isProtected(b.getWorld().getName(), b.getX(), b.getY(), b.getZ())) {
			var sbm = VoiidCountdownTimer.getSpawnBookManager();
			if (sbm != null) {
				try {
					org.bukkit.block.BlockState state = b.getState();
					if (state instanceof Lectern) {
						sbm.refreshLecternBook((Lectern) state);
					}
				} catch (Throwable ignored) {
				}
			}
		}
	}
}
