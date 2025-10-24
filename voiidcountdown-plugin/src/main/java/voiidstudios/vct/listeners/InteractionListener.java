package voiidstudios.vct.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.EquipmentSlot;
import voiidstudios.vct.VoiidCountdownTimer;

/**
 * Listens for player interactions with the vanilla Interaction entity.
 * This is useful for setups that use invisible interaction hitboxes as click targets.
 *
 * Notes:
 * - Uses a name-based check for EntityType to remain forward/backward compatible.
 * - Does not cancel by default; only detects the interaction.
 */
public class InteractionListener implements Listener {
    private final VoiidCountdownTimer plugin;

    public InteractionListener(VoiidCountdownTimer plugin) {
        this.plugin = plugin;
    }

    private boolean isInteractionEntity(Entity entity) {
        // Prefer enum when available, but fall back to name check to be resilient
        try {
            if (entity.getType() == EntityType.INTERACTION) return true;
        } catch (Throwable ignored) {
            // Older API or unexpected environment; use name-based check
        }
        return "INTERACTION".equalsIgnoreCase(entity.getType().name());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // Process only main-hand interactions to avoid duplicate fire from off-hand
        try {
            if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) return;
        } catch (Throwable ignored) {}
        Entity clicked = event.getRightClicked();
        if (!isInteractionEntity(clicked)) return;
        Player player = event.getPlayer();
        voiidstudios.vct.managers.InteractionActionManager mgr = VoiidCountdownTimer.getInteractionActionManager();
        if (mgr != null && mgr.isEnabled()) {
            mgr.processInteraction(player, clicked, event);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        // Process only main-hand interactions to avoid duplicate fire from off-hand
        try {
            if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) return;
        } catch (Throwable ignored) {}
        Entity clicked = event.getRightClicked();
        if (!isInteractionEntity(clicked)) return;

        Player player = event.getPlayer();
        voiidstudios.vct.managers.InteractionActionManager mgr = VoiidCountdownTimer.getInteractionActionManager();
        if (mgr != null && mgr.isEnabled()) {
            mgr.processInteraction(player, clicked, event);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onGenericGameEvent(GenericGameEvent event) {
        // Filter for the specific game event name "player_interacted_with_entity"
        boolean isPlayerInteractedWithEntity = false;
        try {
            // Attempt via key: minecraft:player_interacted_with_entity
            Object gameEvent = event.getEvent();
            java.lang.reflect.Method getKey = gameEvent.getClass().getMethod("getKey");
            Object namespacedKey = getKey.invoke(gameEvent);
            if (namespacedKey != null) {
                String val = null;
                try {
                    // Spigot NamespacedKey#getKey returns path without namespace
                    java.lang.reflect.Method getKeyStr = namespacedKey.getClass().getMethod("getKey");
                    val = String.valueOf(getKeyStr.invoke(namespacedKey));
                } catch (Throwable t) {
                    try {
                        // Fallback to getKey/key or value-style APIs
                        java.lang.reflect.Method value = namespacedKey.getClass().getMethod("value");
                        val = String.valueOf(value.invoke(namespacedKey));
                    } catch (Throwable t2) {
                        try {
                            java.lang.reflect.Method toString = namespacedKey.getClass().getMethod("toString");
                            val = String.valueOf(toString.invoke(namespacedKey));
                        } catch (Throwable ignored2) {}
                    }
                }
                if (val != null && val.toLowerCase(java.util.Locale.ROOT).contains("player_interacted_with_entity")) {
                    isPlayerInteractedWithEntity = true;
                }
            }
        } catch (Throwable ignored) {}

        if (!isPlayerInteractedWithEntity) return;

        Entity source = event.getEntity();
        if (source == null || !isInteractionEntity(source)) return;

        try {
            plugin.getLogger().fine(() -> "game_event player_interacted_with_entity on INTERACTION @ " + source.getLocation());
        } catch (Throwable t) {
            // no-op
        }
    }
}
