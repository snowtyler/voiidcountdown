package voiidstudios.vct.managers;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.configs.MainConfigManager;

import java.util.Locale;

/**
 * Centralizes configuration and simple policies for global loot tweaks.
 */
public class LootModifierManager {
    public enum ApplyScope { ALL, CHESTS, ENTITIES, FISHING, BLOCKS }

    private boolean enabled;
    private Material targetItem;
    private double chanceMultiplier;
    private double quantityMultiplier;
    private int minStack;
    private int maxStack;
    private ApplyScope applyScope;

    public LootModifierManager(@NotNull VoiidCountdownTimer plugin) {
        reload();
    }

    public final void reload() {
        MainConfigManager cfg = VoiidCountdownTimer.getConfigsManager().getMainConfigManager();
        this.enabled = cfg.isLootTweaksEnabled();
        String itemName = cfg.getLootTweaksItem();
        this.targetItem = itemName != null ? Material.matchMaterial(itemName.toUpperCase(Locale.ROOT)) : null;
        this.chanceMultiplier = Math.max(0.0D, cfg.getLootTweaksChanceMultiplier());
        this.quantityMultiplier = Math.max(0.0D, cfg.getLootTweaksQuantityMultiplier());
        this.minStack = Math.max(0, cfg.getLootTweaksMinStack());
        this.maxStack = Math.max(this.minStack, cfg.getLootTweaksMaxStack());
        this.applyScope = parseScope(cfg.getLootTweaksApplyTo());
    }

    private ApplyScope parseScope(String raw) {
        if (raw == null) return ApplyScope.ALL;
        try {
            return ApplyScope.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ApplyScope.ALL;
        }
    }

    public boolean isEnabled() { return enabled && targetItem != null; }
    public Material getTargetItem() { return targetItem; }
    public double getChanceMultiplier() { return chanceMultiplier; }
    public double getQuantityMultiplier() { return quantityMultiplier; }
    public int getMinStack() { return minStack; }
    public int getMaxStack() { return maxStack; }
    public ApplyScope getApplyScope() { return applyScope; }
}
