package voiidstudios.vct.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.configs.model.CustomConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stores and checks protected lectern locations. Backed by lecterns.yml in the plugin data folder.
 */
public class LecternProtectionManager {
    private final CustomConfig configFile;
    private final Set<String> protectedKeys = new HashSet<>();

    public LecternProtectionManager(VoiidCountdownTimer plugin) {
        this.configFile = new CustomConfig("lecterns.yml", plugin, null, false);
        this.configFile.registerConfig();
        reload();
    }

    public void reload() {
        this.configFile.reloadConfig();
        this.protectedKeys.clear();
        FileConfiguration cfg = this.configFile.getConfig();
        List<String> list = cfg.getStringList("protected");
        if (list != null) {
            for (String s : list) {
                if (s != null && !s.trim().isEmpty()) {
                    protectedKeys.add(s.trim());
                }
            }
        }
    }

    public boolean isProtected(String world, int x, int y, int z) {
        return protectedKeys.contains(serialize(world, x, y, z));
    }

    public boolean isProtected(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        return isProtected(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public boolean protect(String world, int x, int y, int z) {
        String key = serialize(world, x, y, z);
        if (protectedKeys.contains(key)) return false;
        protectedKeys.add(key);
        save();
        return true;
    }

    public boolean unprotect(String world, int x, int y, int z) {
        String key = serialize(world, x, y, z);
        if (!protectedKeys.remove(key)) return false;
        save();
        return true;
    }

    public Location parseKey(String key) {
        if (key == null) return null;
        String[] parts = key.split(";", 4);
        if (parts.length != 4) return null;
        World w = Bukkit.getWorld(parts[0]);
        if (w == null) return null;
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(w, x, y, z);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void save() {
        FileConfiguration cfg = this.configFile.getConfig();
        cfg.set("protected", new java.util.ArrayList<>(protectedKeys));
        this.configFile.saveConfig();
    }

    private static String serialize(String world, int x, int y, int z) {
        String w = (world == null) ? "world" : world;
        return w + ";" + x + ";" + y + ";" + z;
    }

    /**
     * Returns all protected lectern locations that currently resolve to loaded worlds.
     */
    public java.util.List<Location> getAllProtectedLocations() {
        java.util.List<Location> list = new java.util.ArrayList<>();
        for (String key : protectedKeys) {
            Location loc = parseKey(key);
            if (loc != null) list.add(loc);
        }
        return list;
    }
}
