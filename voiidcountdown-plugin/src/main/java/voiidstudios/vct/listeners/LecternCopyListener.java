package voiidstudios.vct.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.managers.LecternProtectionManager;

public class LecternCopyListener implements Listener {
	public LecternCopyListener() {}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onTakeLecternBook(PlayerTakeLecternBookEvent event) {
		Block block = event.getLectern().getBlock();
		LecternProtectionManager mgr = VoiidCountdownTimer.getLecternProtectionManager();
		if (mgr == null) return;

		boolean isProtected = mgr.isProtected(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
		if (!isProtected) return; // Not protected: allow normal behavior

		Player player = event.getPlayer();

		// Always prevent taking the original from a protected lectern
		event.setCancelled(true);

		// If the player has a Book and Quill, replace it with the actual prophecy book from the lectern
		ItemStack writable = findBookAndQuill(player);
		if (writable == null) {
			VoiidCountdownTimer.getMessagesManager().sendConfigMessage(player, "Messages.lectern.copy.noQuill", true, null);
			return;
		}

		ItemStack written = getLecternBook(event.getLectern());
		if (written == null || written.getType() != Material.WRITTEN_BOOK || !(written.getItemMeta() instanceof BookMeta)) {
			VoiidCountdownTimer.getMessagesManager().sendConfigMessage(player, "Messages.lectern.copy.noBook", true, null);
			return;
		}

		ItemStack clone = written.clone();
		// Replace the first Book and Quill found with the written book
		boolean replaced = replaceFirstBookAndQuill(player, clone);
		if (!replaced) {
			// Fallback: try adding to inventory
			player.getInventory().addItem(clone);
		}
		VoiidCountdownTimer.getMessagesManager().sendConfigMessage(player, "Messages.lectern.copy.success", true, null);
	}

	private ItemStack getLecternBook(Lectern lectern) {
		try {
			ItemStack item = lectern.getInventory().getItem(0);
			if (item != null && item.getType() == Material.WRITTEN_BOOK) return item;
		} catch (Throwable ignored) {}
		return null;
	}

	private ItemStack findBookAndQuill(Player player) {
		if (player == null) return null;
		for (ItemStack it : player.getInventory().getContents()) {
			if (it != null && it.getType() == Material.WRITABLE_BOOK) {
				return it;
			}
		}
		return null;
	}

	private boolean replaceFirstBookAndQuill(Player player, ItemStack replacement) {
		for (int i = 0; i < player.getInventory().getSize(); i++) {
			ItemStack it = player.getInventory().getItem(i);
			if (it != null && it.getType() == Material.WRITABLE_BOOK) {
				replacement.setAmount(it.getAmount());
				player.getInventory().setItem(i, replacement);
				return true;
			}
		}
		return false;
	}
}
