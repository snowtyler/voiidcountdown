package voiidstudios.vct.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import voiidstudios.vct.VoiidCountdownTimer;

import java.util.Collection;

public class ItemFrameProtectionListener implements Listener {
    private boolean isAnchor(Block block) {
        var cfg = VoiidCountdownTimer.getConfigsManager().getMainConfigManager();
        World world = Bukkit.getWorld(cfg.getItemFrameSensorWorld());
        if (world == null) return false;
        if (!block.getWorld().equals(world)) return false;
        return block.getX() == cfg.getItemFrameSensorX() && block.getY() == cfg.getItemFrameSensorY() && block.getZ() == cfg.getItemFrameSensorZ();
    }

    private boolean isProtectedFrame(Entity e) {
        if (!(e instanceof ItemFrame)) return false;
        var cfg = VoiidCountdownTimer.getConfigsManager().getMainConfigManager();
        if (!cfg.isItemFrameSensorEnabled() || !cfg.isItemFrameSensorProtectFrame()) return false;
        World world = Bukkit.getWorld(cfg.getItemFrameSensorWorld());
        if (world == null || !e.getWorld().equals(world)) return false;

        Block anchor = world.getBlockAt(cfg.getItemFrameSensorX(), cfg.getItemFrameSensorY(), cfg.getItemFrameSensorZ());
        Location center = anchor.getLocation().add(0.5, 0.5, 0.5);
        Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, 3.0, 3.0, 3.0, en -> en instanceof ItemFrame);
        String facingFilter = cfg.getItemFrameSensorFacing();
        BlockFace required = null;
        if (facingFilter != null && !facingFilter.trim().isEmpty()) {
            try { required = BlockFace.valueOf(facingFilter.trim().toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        for (Entity en : nearby) {
            if (!(en instanceof ItemFrame)) continue;
            ItemFrame f = (ItemFrame) en;
            BlockFace facingOut;
            try { facingOut = f.getFacing(); } catch (Throwable ex) { facingOut = null; }
            if (required != null && facingOut != null && !facingOut.equals(required)) continue;
            Block frameBlock = f.getLocation().getBlock();
            Block attachedTo = frameBlock.getRelative(facingOut != null ? facingOut.getOppositeFace() : BlockFace.SELF);
            boolean matches = isAnchor(attachedTo) || isAnchor(frameBlock);
            if (matches && en.getUniqueId().equals(e.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        var cfg = VoiidCountdownTimer.getConfigsManager().getMainConfigManager();
        if (!cfg.isItemFrameSensorEnabled() || !cfg.isItemFrameSensorProtectAnchor()) return;
        if (isAnchor(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        var cfg = VoiidCountdownTimer.getConfigsManager().getMainConfigManager();
        if (!cfg.isItemFrameSensorEnabled()) return;
        if (cfg.isItemFrameSensorProtectAnchor()) {
            e.blockList().removeIf(this::isAnchor);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        var cfg = VoiidCountdownTimer.getConfigsManager().getMainConfigManager();
        if (!cfg.isItemFrameSensorEnabled() || !cfg.isItemFrameSensorProtectAnchor()) return;
        e.getBlocks().removeIf(this::isAnchor);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        var cfg = VoiidCountdownTimer.getConfigsManager().getMainConfigManager();
        if (!cfg.isItemFrameSensorEnabled() || !cfg.isItemFrameSensorProtectAnchor()) return;
        e.getBlocks().removeIf(this::isAnchor);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent e) {
        var cfg = VoiidCountdownTimer.getConfigsManager().getMainConfigManager();
        if (!cfg.isItemFrameSensorEnabled() || !cfg.isItemFrameSensorProtectFrame()) return;
        if (isProtectedFrame(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent e) {
        var cfg = VoiidCountdownTimer.getConfigsManager().getMainConfigManager();
        if (!cfg.isItemFrameSensorEnabled() || !cfg.isItemFrameSensorProtectFrame()) return;
        if (isProtectedFrame(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        var cfg = VoiidCountdownTimer.getConfigsManager().getMainConfigManager();
        if (!cfg.isItemFrameSensorEnabled() || !cfg.isItemFrameSensorProtectFrame()) return;
        if (!isProtectedFrame(e.getEntity())) return;

        // Allow players to punch the frame to pop the item out when present,
        // but prevent the frame itself from being destroyed when empty.
        if (e.getEntity() instanceof ItemFrame) {
            ItemFrame frame = (ItemFrame) e.getEntity();
            boolean hasItem = frame.getItem() != null && frame.getItem().getType() != org.bukkit.Material.AIR;
            if (!hasItem) {
                // Empty frame: cancel damage so the frame isn't destroyed
                e.setCancelled(true);
            } else {
                // Has item: allow this hit to remove the item, do not cancel
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageGeneric(EntityDamageEvent e) {
        var cfg = VoiidCountdownTimer.getConfigsManager().getMainConfigManager();
        if (!cfg.isItemFrameSensorEnabled() || !cfg.isItemFrameSensorProtectFrame()) return;
        if (!isProtectedFrame(e.getEntity())) return;

        // Allow entity-vs-entity damage to be handled by onEntityDamage (it permits popping items)
        if (e instanceof EntityDamageByEntityEvent) return;

        // Protect against environmental damage like explosions, fire, etc.
        EntityDamageEvent.DamageCause cause = e.getCause();
        switch (cause) {
            case BLOCK_EXPLOSION:
            case ENTITY_EXPLOSION:
            case FIRE:
            case FIRE_TICK:
            case LAVA:
            case CONTACT:
            case CRAMMING:
            case CUSTOM:
                e.setCancelled(true);
                break;
            default:
                // Leave other causes alone; onEntityDamageByEntity will manage player/projectile hits
                break;
        }
    }
}
