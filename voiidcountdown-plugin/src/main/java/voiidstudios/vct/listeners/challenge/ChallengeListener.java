package voiidstudios.vct.listeners.challenge;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.PlayerInventory;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.challenges.ChallengeManager;

import java.util.EnumMap;
import java.util.Map;

public class ChallengeListener implements Listener {
    private final ChallengeManager challengeManager;
    private final VoiidCountdownTimer plugin;

    public ChallengeListener(ChallengeManager challengeManager, VoiidCountdownTimer plugin) {
        this.challengeManager = challengeManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        challengeManager.handleEntityKill(event.getEntityType(), event.getEntity().getScoreboardTags(), killer);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        ItemStack stack = event.getItem() == null ? null : event.getItem().getItemStack();
        if (stack == null) {
            return;
        }
        int amount = Math.max(1, stack.getAmount());
        challengeManager.handleItemAcquire(stack.getType(), player, amount);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) {
            result = event.getRecipe() != null ? event.getRecipe().getResult() : null;
        }
        if (result == null || result.getType() == Material.AIR) return;

        int crafts = 1;
        if (event.isShiftClick()) {
            int min = Integer.MAX_VALUE;
            Inventory inv = event.getInventory();
            if (inv instanceof CraftingInventory) {
                ItemStack[] matrix = ((CraftingInventory) inv).getMatrix();
                for (ItemStack is : matrix) {
                    if (is == null || is.getType() == Material.AIR) continue;
                    min = Math.min(min, Math.max(0, is.getAmount()));
                }
            }
            if (min == Integer.MAX_VALUE) min = 1;
            crafts = Math.max(1, min);
        }
        int amount = Math.max(1, result.getAmount()) * crafts;
        challengeManager.handleItemAcquire(result.getType(), player, amount);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (!shouldTrackInventoryInteraction(event.getView(), event.getAction())) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Skip crafting result; CraftItemEvent already handles crafting gains
        Inventory clicked = event.getClickedInventory();
        if (clicked != null
            && (clicked.getType() == InventoryType.CRAFTING || clicked.getType() == InventoryType.WORKBENCH)
            && event.getSlotType() == SlotType.RESULT) {
            return;
        }

        Map<Material, Integer> before = snapshotPlayerInventory(player.getInventory());
        scheduleInventoryDiffCheck(player, before);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        InventoryView view = event.getView();
        if (!shouldTrackInventoryInteraction(view, InventoryAction.UNKNOWN)) {
            return;
        }

        // Only care when any of the target slots belong to the player's inventory
        int topSize = view.getTopInventory() != null ? view.getTopInventory().getSize() : 0;
        boolean affectsPlayerInventory = event.getRawSlots().stream().anyMatch(slot -> slot >= topSize);
        if (!affectsPlayerInventory) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Map<Material, Integer> before = snapshotPlayerInventory(player.getInventory());
        scheduleInventoryDiffCheck(player, before);
    }

    private boolean shouldTrackInventoryInteraction(InventoryView view, InventoryAction action) {
        if (view == null) {
            return false;
        }

        Inventory top = view.getTopInventory();
        if (top == null) {
            return false;
        }

        // Skip interactions when no external container is open (player inventory view)
        if (top.getType() == InventoryType.CRAFTING) {
            return false;
        }

        if (action == InventoryAction.NOTHING
            || action == InventoryAction.DROP_ALL_CURSOR
            || action == InventoryAction.DROP_ONE_CURSOR
            || action == InventoryAction.DROP_ALL_SLOT
            || action == InventoryAction.DROP_ONE_SLOT) {
            return false;
        }

        return true;
    }

    private Map<Material, Integer> snapshotPlayerInventory(PlayerInventory inventory) {
        Map<Material, Integer> totals = new EnumMap<>(Material.class);
        accumulateStacks(totals, inventory.getStorageContents());
        accumulateStacks(totals, inventory.getArmorContents());

        ItemStack offHand = inventory.getItemInOffHand();
        if (offHand != null && offHand.getType() != Material.AIR) {
            totals.merge(offHand.getType(), offHand.getAmount(), Integer::sum);
        }

        return totals;
    }

    private void accumulateStacks(Map<Material, Integer> totals, ItemStack[] stacks) {
        if (stacks == null) {
            return;
        }
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            totals.merge(stack.getType(), stack.getAmount(), Integer::sum);
        }
    }

    private void scheduleInventoryDiffCheck(Player player, Map<Material, Integer> before) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Map<Material, Integer> after = snapshotPlayerInventory(player.getInventory());
            for (Map.Entry<Material, Integer> entry : after.entrySet()) {
                Material material = entry.getKey();
                int afterAmount = entry.getValue();
                int beforeAmount = before.getOrDefault(material, 0);
                int gained = afterAmount - beforeAmount;
                if (gained > 0) {
                    challengeManager.handleItemAcquire(material, player, gained);
                }
            }
        });
    }
}
