package voiidstudios.vct.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.managers.SpawnBookManager;

public class SpawnBookInventoryListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        SpawnBookManager manager = VoiidCountdownTimer.getSpawnBookManager();
        if (manager == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        if (!isRelevant(manager, event.getCurrentItem(), event.getCursor())) {
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton >= 0) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
                if (!isRelevant(manager, hotbarItem, null)) {
                    return;
                }
            } else {
                return;
            }
        }

        scheduleUpdate(player, manager);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        SpawnBookManager manager = VoiidCountdownTimer.getSpawnBookManager();
        if (manager == null) {
            return;
        }

        ItemStack cursor = event.getCursor();
        ItemStack oldCursor = event.getOldCursor();
        if (!isRelevant(manager, cursor, oldCursor)) {
            return;
        }

        scheduleUpdate((Player) event.getWhoClicked(), manager);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        SpawnBookManager manager = VoiidCountdownTimer.getSpawnBookManager();
        if (manager == null) {
            return;
        }

        if (!manager.isSpawnBookItem(event.getItem().getItemStack())) {
            return;
        }

        scheduleUpdate((Player) event.getEntity(), manager);
    }

    private boolean isRelevant(SpawnBookManager manager, ItemStack primary, ItemStack secondary) {
        if (manager.isSpawnBookItem(primary)) {
            return true;
        }
        return manager.isSpawnBookItem(secondary);
    }

    private void scheduleUpdate(Player player, SpawnBookManager manager) {
        if (player == null) {
            return;
        }
        Bukkit.getScheduler().runTask(VoiidCountdownTimer.getInstance(), () -> {
            try {
                manager.updatePlayerBook(player);
            } catch (Throwable ignored) {
            }
        });
    }
}

