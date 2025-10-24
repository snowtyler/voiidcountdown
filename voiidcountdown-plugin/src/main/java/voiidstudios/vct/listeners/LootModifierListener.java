package voiidstudios.vct.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.jetbrains.annotations.NotNull;
import voiidstudios.vct.managers.LootModifierManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Adjusts the frequency and stack size of a configured item across loot-table-based sources.
 *
 * Notes:
 * - This operates on the already-rolled loot list; to increase "chance", we probabilistically add extra copies
 *   of the item proportional to chance_multiplier. For multipliers < 1, we probabilistically remove some copies.
 * - Quantity multiplier applies to each item stack of the target item, clamped to min/max.
 */
public class LootModifierListener implements Listener {
    private final LootModifierManager manager;

    public LootModifierListener(@NotNull LootModifierManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLootGenerate(@NotNull LootGenerateEvent event) {
        if (!manager.isEnabled()) return;

        LootModifierManager.ApplyScope scope = manager.getApplyScope();
        switch (event.getLootContext().getLootedEntity() != null ? "ENTITIES" : sourceFromContext(event.getLootContext())) {
            case "CHESTS":
                if (!(scope == LootModifierManager.ApplyScope.ALL || scope == LootModifierManager.ApplyScope.CHESTS)) return;
                break;
            case "ENTITIES":
                if (!(scope == LootModifierManager.ApplyScope.ALL || scope == LootModifierManager.ApplyScope.ENTITIES)) return;
                break;
            case "FISHING":
                if (!(scope == LootModifierManager.ApplyScope.ALL || scope == LootModifierManager.ApplyScope.FISHING)) return;
                break;
            case "BLOCKS":
                if (!(scope == LootModifierManager.ApplyScope.ALL || scope == LootModifierManager.ApplyScope.BLOCKS)) return;
                break;
        }

        Material target = manager.getTargetItem();
        if (target == null) return;

        List<ItemStack> loot = event.getLoot();
        if (loot == null || loot.isEmpty()) return;

        double chanceMul = manager.getChanceMultiplier();
        double qtyMul = manager.getQuantityMultiplier();
        int min = manager.getMinStack();
        int max = manager.getMaxStack();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // First pass: modify quantity and count occurrences
        int occurrences = 0;
        for (ItemStack stack : loot) {
            if (stack == null) continue;
            if (stack.getType() != target) continue;
            occurrences++;
            if (qtyMul >= 0.0D) {
                int newAmount = (int) Math.round(stack.getAmount() * qtyMul);
                newAmount = Math.max(min, Math.min(max, newAmount));
                if (newAmount <= 0) {
                    stack.setAmount(0); // will be filtered in cleanup below
                } else {
                    stack.setAmount(newAmount);
                }
            }
        }

        // Second pass: adjust chance by removing or adding extra copies
        if (occurrences > 0) {
            if (chanceMul < 1.0D) {
                double keepProb = Math.max(0.0D, chanceMul);
                Iterator<ItemStack> it = loot.iterator();
                while (it.hasNext()) {
                    ItemStack s = it.next();
                    if (s == null || s.getType() != target) continue;
                    if (rng.nextDouble() > keepProb) {
                        it.remove();
                    }
                }
            } else if (chanceMul > 1.0D) {
                int baseCopies = (int) Math.floor(chanceMul) - 1; // guaranteed extra copies per existing occurrence
                double extraProb = chanceMul - Math.floor(chanceMul);
                List<ItemStack> additions = new ArrayList<>();
                for (ItemStack s : loot) {
                    if (s == null || s.getType() != target) continue;
                    // guaranteed copies
                    for (int i = 0; i < baseCopies; i++) {
                        additions.add(clampedCopy(s, min, max));
                    }
                    // fractional copy
                    if (extraProb > 0 && rng.nextDouble() < extraProb) {
                        additions.add(clampedCopy(s, min, max));
                    }
                }
                if (!additions.isEmpty()) {
                    loot.addAll(additions);
                }
            }
        }

        // Cleanup: remove zero-amount stacks if any resulted from clamping
        loot.removeIf(is -> is == null || is.getType() == Material.AIR || is.getAmount() <= 0);
    }

    private ItemStack clampedCopy(ItemStack original, int min, int max) {
        int amount = Math.max(min, Math.min(max, original.getAmount()));
        ItemStack copy = new ItemStack(original.getType(), Math.max(1, amount));
        if (original.hasItemMeta()) {
            copy.setItemMeta(original.getItemMeta());
        }
        return copy;
    }

    // Heuristic for source classification for apply_to matching
    private String sourceFromContext(LootContext ctx) {
        try {
            // If looted entity is null, it may be a chest, block, or fishing
            // Bukkit doesn't expose a direct enum; we can heuristically inspect location and killer/fisher
            if (ctx.getKiller() != null || ctx.getLootedEntity() != null) return "ENTITIES";
            if (ctx.getLocation() != null && ctx.getLocation().getBlock() != null) {
                Material type = ctx.getLocation().getBlock().getType();
                String name = type.name().toUpperCase(Locale.ROOT);
                if (name.contains("CHEST") || name.contains("BARREL") || name.contains("SHULKER") || name.contains("CHEST_MINECART")) {
                    return "CHESTS";
                }
                return "BLOCKS";
            }
            return "CHESTS"; // default most common
        } catch (Throwable t) {
            return "ALL";
        }
    }
}
