/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Mob
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.entity.CreatureSpawnEvent
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityPickupItemEvent
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.inventory.InventoryOpenEvent
 *  org.bukkit.event.player.AsyncPlayerChatEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerInteractAtEntityEvent
 *  org.bukkit.event.player.PlayerInteractEntityEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerPickupItemEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.event.player.PlayerSwapHandItemsEvent
 *  org.bukkit.event.player.PlayerTeleportEvent
 *  org.bukkit.event.player.PlayerToggleSneakEvent
 *  org.bukkit.event.player.PlayerToggleSprintEvent
 *  voiidstudios.vct.VoiidCountdownTimer
 *  voiidstudios.vct.managers.FreezeManager
 */
package voiidstudios.vct.listeners;

import java.lang.reflect.Method;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.managers.FreezeManager;

public class FreezeListener
implements Listener {
    private final FreezeManager freezeManager = VoiidCountdownTimer.getFreezeManager();

    public FreezeListener(VoiidCountdownTimer plugin) {
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (this.freezeManager.isFrozen() && this.freezeManager.isSilentNotifications()) {
            try {
                event.setJoinMessage(null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        this.freezeManager.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        this.freezeManager.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (this.freezeManager.isFrozen() && this.freezeManager.isSilentNotifications()) {
            try {
                event.setQuitMessage(null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        this.freezeManager.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (this.freezeManager.isFrozen() && this.freezeManager.isSilentNotifications()) {
            try {
                event.setDeathMessage(null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!this.freezeManager.shouldCancelPlayer(player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onTeleport(PlayerTeleportEvent event) {
        this.freezeManager.handleTeleport(event.getPlayer(), event);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!this.freezeManager.shouldCancelPlayer(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onSprint(PlayerToggleSprintEvent event) {
        if (!this.freezeManager.shouldCancelPlayer(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onInteract(PlayerInteractEvent event) {
        if (!this.freezeManager.shouldCancelPlayer(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        this.freezeManager.notifyBlocked(event.getPlayer(), "Messages.freezeBlockedAction");
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!this.freezeManager.shouldCancelPlayer(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        this.freezeManager.notifyBlocked(event.getPlayer(), "Messages.freezeBlockedAction");
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (!this.freezeManager.shouldCancelPlayer(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        this.freezeManager.notifyBlocked(event.getPlayer(), "Messages.freezeBlockedAction");
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!this.freezeManager.shouldCancelPlayer(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        this.freezeManager.notifyBlocked(event.getPlayer(), "Messages.freezeBlockedAction");
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!this.freezeManager.shouldCancelPlayer(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        this.freezeManager.notifyBlocked(event.getPlayer(), "Messages.freezeBlockedAction");
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!this.freezeManager.shouldCancelPlayer(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        this.freezeManager.notifyBlocked(event.getPlayer(), "Messages.freezeBlockedInventory");
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getPlayer();
        if (!this.freezeManager.shouldCancelPlayer(player)) {
            return;
        }
        event.setCancelled(true);
        this.freezeManager.notifyBlocked(player, "Messages.freezeBlockedInventory");
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getWhoClicked();
        if (!this.freezeManager.shouldCancelPlayer(player)) {
            return;
        }
        event.setCancelled(true);
        this.freezeManager.notifyBlocked(player, "Messages.freezeBlockedInventory");
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getWhoClicked();
        if (!this.freezeManager.shouldCancelPlayer(player)) {
            return;
        }
        event.setCancelled(true);
        this.freezeManager.notifyBlocked(player, "Messages.freezeBlockedInventory");
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!this.freezeManager.shouldCancelPlayer(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        this.freezeManager.notifyBlocked(event.getPlayer(), "Messages.freezeBlockedAction");
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onEntityPickup(EntityPickupItemEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        if (!this.freezeManager.shouldCancelPlayer(player)) {
            return;
        }
        event.setCancelled(true);
        this.freezeManager.notifyBlocked(player, "Messages.freezeBlockedAction");
    }

    // Removed deprecated PlayerPickupItemEvent in favor of EntityPickupItemEvent above

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!this.freezeManager.shouldCancelPlayer(player)) {
            return;
        }
        event.setCancelled(true);
        this.freezeManager.notifyBlocked(player, "Messages.freezeBlockedChat");
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();
        boolean cancel = false;
        if (damager instanceof Player && this.freezeManager.shouldCancelPlayer((Player)damager)) {
            cancel = true;
            this.freezeManager.notifyBlocked((Player)damager, "Messages.freezeBlockedAction");
        }
        if (victim instanceof Player && this.freezeManager.shouldCancelPlayer((Player)victim)) {
            cancel = true;
        }
        if (cancel) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Mob)) {
            return;
        }
        if (!VoiidCountdownTimer.getFreezeManager().isFrozen()) {
            return;
        }
        try {
            Method method = this.freezeManager.getClass().getMethod("shouldPreventMobSpawn", new Class[0]);
            Object val = method.invoke((Object)this.freezeManager, new Object[0]);
            if (val instanceof Boolean && ((Boolean)val).booleanValue()) {
                event.setCancelled(true);
                return;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        this.freezeManager.handleMobSpawn((Mob)event.getEntity());
    }
}
