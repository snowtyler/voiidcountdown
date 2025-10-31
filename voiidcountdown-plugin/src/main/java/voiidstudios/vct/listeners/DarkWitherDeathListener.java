package voiidstudios.vct.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fires when a Wither tagged "DarkWither" dies and runs configured commands.
 * Commands are executed as console in order, with no delays.
 *
 * Config path: DarkWither.on_kill_commands (String list)
 */
public class DarkWitherDeathListener implements Listener {
    private static final String WITHER_TAG = "DarkWither";
    private final JavaPlugin plugin;

    public DarkWitherDeathListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWitherDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Wither)) return;
        if (!entity.getScoreboardTags().contains(WITHER_TAG)) return;

        Object rawList = plugin.getConfig().get("DarkWither.on_kill_commands");
        if (!(rawList instanceof List)) return;
        List<?> script = (List<?>) rawList;
        if (script.isEmpty()) return;

        // Capture simple context for optional placeholder replacement
        Location loc = entity.getLocation();
        World world = entity.getWorld();
        String worldName = world != null ? world.getName() : "world";
        String x = String.format(java.util.Locale.ROOT, "%.2f", loc.getX());
        String y = String.format(java.util.Locale.ROOT, "%.2f", loc.getY());
        String z = String.format(java.util.Locale.ROOT, "%.2f", loc.getZ());
        String killerName = event.getEntity().getKiller() != null ? event.getEntity().getKiller().getName() : "";

        // Run next tick to be safely post-death
        long offset = 1L; // start next tick to ensure post-death

        for (Object item : script) {
            if (item == null) continue;

            if (item instanceof String) {
                String s = ((String) item).trim();
                if (s.isEmpty()) continue;

                // Support textual delays like: "delay 40" or "wait 40"
                String lower = s.toLowerCase(Locale.ROOT);
                if (lower.startsWith("delay ") || lower.startsWith("wait ")) {
                    String numStr = s.substring(s.indexOf(' ') + 1).trim();
                    try {
                        long d = Long.parseLong(numStr);
                        if (d > 0) offset += d;
                    } catch (NumberFormatException ignored) {}
                    continue;
                }

                // It's a normal command string
                String cmd = s
                        .replace("%WORLD%", worldName)
                        .replace("%X%", x)
                        .replace("%Y%", y)
                        .replace("%Z%", z)
                        .replace("%KILLER%", killerName);

                long runAt = offset;
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd), runAt);
                continue;
            }

            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) item;
                // Accepted keys: command (String), delay (Number)
                long d = 0L;
                Object dObj = map.get("delay");
                if (dObj instanceof Number) {
                    d = Math.max(0L, ((Number) dObj).longValue());
                } else if (dObj instanceof String) {
                    try { d = Math.max(0L, Long.parseLong(((String) dObj).trim())); } catch (NumberFormatException ignored) {}
                }
                if (d > 0) offset += d; // delay before this command

                Object cObj = map.get("command");
                if (cObj instanceof String) {
                    String s = ((String) cObj).trim();
                    if (!s.isEmpty()) {
                        String cmd = s
                                .replace("%WORLD%", worldName)
                                .replace("%X%", x)
                                .replace("%Y%", y)
                                .replace("%Z%", z)
                                .replace("%KILLER%", killerName);
                        long runAt = offset;
                        Bukkit.getScheduler().runTaskLater(plugin, () ->
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd), runAt);
                    }
                } else {
                    // Map with only delay acts as a wait step
                    // (no command to execute)
                }
                continue;
            }

            // Unrecognized item type: ignore
        }
    }
}
