package voiidstudios.vct.managers;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitTask;
import voiidstudios.vct.VoiidCountdownTimer;

import java.util.*;

/**
 * Polls a configured item frame and toggles permission/commands based on whether a specific item is displayed.
 */
public class ItemFrameSensorManager {
    private final VoiidCountdownTimer plugin;
    private final String worldName;
    private final int x, y, z;
    private final String facingName; // optional
    private final String materialName;
    private final int periodTicks;
    private final boolean fireOnStartup;
    private final String permissionToGrant; // optional
    private final List<String> onPresentCommands;
    private final List<String> onAbsentCommands;

    private BukkitTask task;
    private Boolean activeState = null; // null until first evaluation
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public ItemFrameSensorManager(VoiidCountdownTimer plugin) {
        this.plugin = plugin;
        var cfg = VoiidCountdownTimer.getConfigsManager().getMainConfigManager();
        this.worldName = cfg.getItemFrameSensorWorld();
        this.x = cfg.getItemFrameSensorX();
        this.y = cfg.getItemFrameSensorY();
        this.z = cfg.getItemFrameSensorZ();
        this.facingName = cfg.getItemFrameSensorFacing();
        this.materialName = cfg.getItemFrameSensorMaterial();
        this.periodTicks = Math.max(1, cfg.getItemFrameSensorCheckPeriod());
        this.fireOnStartup = cfg.isItemFrameSensorFireOnStartup();
        this.permissionToGrant = Optional.ofNullable(cfg.getItemFrameSensorPermission()).orElse("").trim();
        var fileCfg = VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getConfig();
        this.onPresentCommands = new ArrayList<>(fileCfg.getStringList("ItemFrameSensor.commands_on_present"));
        this.onAbsentCommands = new ArrayList<>(fileCfg.getStringList("ItemFrameSensor.commands_on_absent"));
    }

    public void startIfEnabled() {
        if (!VoiidCountdownTimer.getConfigsManager().getMainConfigManager().isItemFrameSensorEnabled()) {
            return;
        }
        if (task != null) return;
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, periodTicks);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        // Revoke permission if currently active
        if (Boolean.TRUE.equals(activeState)) {
            setPermissionForAll(false);
        }
        // Clear attachments to avoid leaks
        clearAllAttachments();
        activeState = null;
    }

    private void tick() {
        Boolean isNowActive = detectActiveNullable();
        // If state is unknown (e.g., chunks unloaded), do not flip state
        if (isNowActive == null) return;

        if (activeState == null) {
            activeState = isNowActive;
            // Apply permission state on startup
            if (isNowActive) setPermissionForAll(true);
            if (isNowActive && fireOnStartup) runCommands(onPresentCommands);
            return;
        }
        if (!Objects.equals(activeState, isNowActive)) {
            boolean becameActive = isNowActive;
            activeState = isNowActive;
            if (becameActive) {
                setPermissionForAll(true);
                runCommands(onPresentCommands);
            } else {
                setPermissionForAll(false);
                runCommands(onAbsentCommands);
            }
        }
    }

    /**
     * Detects whether the sensor should be active.
     * Returns:
     *  - TRUE if the expected item is definitively present in the target frame
     *  - FALSE if the frame is definitively present but does not display the item (or empty)
     *  - NULL if the world/chunk area that could contain the frame is not fully loaded (unknown)
     */
    private Boolean detectActiveNullable() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null; // world not loaded -> unknown

        Block anchor = world.getBlockAt(x, y, z);
        if (!isSensorAreaLoaded(anchor)) return null; // area not loaded -> unknown

        BlockFace requiredFace = parseFace(facingName);
        ItemFrame frame = findItemFrameAttachedTo(anchor, requiredFace);
        if (frame == null) return false; // area loaded, frame not found -> definitively inactive

        ItemStack displayed = frame.getItem();
        if (displayed == null || displayed.getType() == Material.AIR) return false;

        Material target;
        try { target = Material.valueOf(materialName.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException | NullPointerException e) { return false; }

        return displayed.getType() == target;
    }

    /**
     * Checks whether the chunk(s) that could contain the item frame are loaded.
     * We consider the anchor block and its 6 adjacent blocks because the frame's
     * entity/block position can be on the anchor or on its outward face.
     */
    private boolean isSensorAreaLoaded(Block anchor) {
        if (anchor == null) return false;
        Block[] candidates = new Block[]{
                anchor,
                anchor.getRelative(BlockFace.NORTH),
                anchor.getRelative(BlockFace.SOUTH),
                anchor.getRelative(BlockFace.EAST),
                anchor.getRelative(BlockFace.WEST),
                anchor.getRelative(BlockFace.UP),
                anchor.getRelative(BlockFace.DOWN)
        };
        for (Block b : candidates) {
            try {
                if (b == null) continue;
                if (!b.getChunk().isLoaded()) return false;
            } catch (Throwable ignored) { return false; }
        }
        return true;
    }

    private ItemFrame findItemFrameAttachedTo(Block anchor, BlockFace requiredFace) {
        Location center = anchor.getLocation().add(0.5, 0.5, 0.5);
        // Use a slightly larger radius to account for entity position variance
        Collection<Entity> nearby = Objects.requireNonNull(center.getWorld())
                .getNearbyEntities(center, 2.5, 2.5, 2.5, e -> e instanceof ItemFrame);
        for (Entity e : nearby) {
            ItemFrame f = (ItemFrame) e;
            try {
                // Derive the anchor block using the frame's facing: the anchor is opposite of facing
                BlockFace facingOut;
                try { facingOut = f.getFacing(); } catch (Throwable ex) { facingOut = null; }
                if (facingOut == null) continue;
            Block frameBlock = f.getLocation().getBlock();
            Block attachedTo = frameBlock.getRelative(facingOut.getOppositeFace());
            boolean matchesAnchor = attachedTo.getLocation().toVector().equals(anchor.getLocation().toVector())
                || frameBlock.getLocation().toVector().equals(anchor.getLocation().toVector());
            if (!matchesAnchor) continue;
                if (requiredFace != null) {
                    // Compare against the outward facing of the frame
                    if (!facingOut.equals(requiredFace)) continue;
                }
                return f;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private BlockFace parseFace(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try { return BlockFace.valueOf(t.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { return null; }
    }

    private void runCommands(List<String> commands) {
        if (commands == null || commands.isEmpty()) return;
        for (String cmd : commands) {
            if (cmd == null || cmd.trim().isEmpty()) continue;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    private void setPermissionForAll(boolean value) {
        if (permissionToGrant == null || permissionToGrant.isEmpty()) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            setPermission(p, value);
        }
    }

    public void setPermission(Player player, boolean value) {
        if (permissionToGrant == null || permissionToGrant.isEmpty()) return;
        PermissionAttachment att = attachments.computeIfAbsent(player.getUniqueId(),
                id -> player.addAttachment(plugin));
        att.setPermission(permissionToGrant, value);
    }

    public void handleJoin(Player player) {
        if (Boolean.TRUE.equals(activeState)) setPermission(player, true);
    }

    private void clearAllAttachments() {
        for (Map.Entry<UUID, PermissionAttachment> e : attachments.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null && e.getValue() != null) {
                try { p.removeAttachment(e.getValue()); } catch (Throwable ignored) {}
            }
        }
        attachments.clear();
    }

    // --- Status reporting ---
    public static class Status {
        public boolean enabled;
        public String worldName;
        public boolean worldFound;
        public int x, y, z;
        public String facingName;
        public String materialName;
        public int periodTicks;
        public boolean fireOnStartup;
        public String permissionToGrant;
        public int nearbyFrames;
        public boolean matchedFrameFound;
        public String matchAnchorType; // "anchor" | "frameBlock" | null
        public String matchedFacing;
        public String displayedItem;
        public Boolean currentActiveState; // last toggled state (may be null)
        public boolean expectedActiveNow; // recomputed
    }

    public Status getStatus() {
        Status s = new Status();
        s.enabled = VoiidCountdownTimer.getConfigsManager().getMainConfigManager().isItemFrameSensorEnabled();
        s.worldName = worldName;
        World world = Bukkit.getWorld(worldName);
        s.worldFound = world != null;
        s.x = x; s.y = y; s.z = z;
        s.facingName = facingName;
        s.materialName = materialName;
        s.periodTicks = periodTicks;
        s.fireOnStartup = fireOnStartup;
        s.permissionToGrant = permissionToGrant;
        s.currentActiveState = activeState;

        if (world == null) {
            s.nearbyFrames = 0;
            s.matchedFrameFound = false;
            s.expectedActiveNow = false;
            return s;
        }

        Block anchor = world.getBlockAt(x, y, z);
        Location center = anchor.getLocation().add(0.5, 0.5, 0.5);
        Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, 3.0, 3.0, 3.0, e -> e instanceof ItemFrame);
        s.nearbyFrames = nearby.size();

        BlockFace requiredFace = parseFace(facingName);
        ItemFrame matched = null;
        String matchType = null;
        for (Entity e : nearby) {
            ItemFrame f = (ItemFrame) e;
            BlockFace facingOut;
            try { facingOut = f.getFacing(); } catch (Throwable ex) { facingOut = null; }
            if (requiredFace != null && facingOut != null && !facingOut.equals(requiredFace)) {
                continue;
            }
            Block frameBlock = f.getLocation().getBlock();
            Block attachedTo = frameBlock.getRelative(facingOut != null ? facingOut.getOppositeFace() : BlockFace.SELF);
            boolean anchorMatch = attachedTo.getLocation().toVector().equals(anchor.getLocation().toVector());
            boolean frameMatch = frameBlock.getLocation().toVector().equals(anchor.getLocation().toVector());
            if (anchorMatch || frameMatch) {
                matched = f;
                matchType = anchorMatch ? "anchor" : "frameBlock";
                break;
            }
        }

        if (matched != null) {
            s.matchedFrameFound = true;
            BlockFace out;
            try { out = matched.getFacing(); } catch (Throwable ex) { out = null; }
            s.matchedFacing = (out != null ? out.name() : null);
            ItemStack displayed = matched.getItem();
            s.displayedItem = (displayed != null ? displayed.getType().name() : null);
            s.matchAnchorType = matchType;
            Material target;
            try { target = Material.valueOf(materialName.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException | NullPointerException e) { target = null; }
            s.expectedActiveNow = displayed != null && target != null && displayed.getType() == target;
        } else {
            s.matchedFrameFound = false;
            s.expectedActiveNow = false;
        }

        return s;
    }
}
