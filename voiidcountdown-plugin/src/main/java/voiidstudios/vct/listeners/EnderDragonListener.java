package voiidstudios.vct.listeners;

import org.bukkit.Bukkit;

import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.java.JavaPlugin;
 

/**
 * Listens for Ender Dragon activity and triggers a datapack function to play music.
 */
public class EnderDragonListener implements Listener {
    private final JavaPlugin plugin;

    public EnderDragonListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // When an Ender Dragon spawns, trigger the music function at the dragon's location
    @EventHandler
    public void onDragonSpawn(EntitySpawnEvent event) {
        Entity e = event.getEntity();
        if (e instanceof EnderDragon) {
            dispatchPlayMusicFunctionAtEntity(e, null);
        }
    }

    // If a player damages the dragon (start of fight), trigger the music function at the dragon for that player
    @EventHandler
    public void onDragonDamaged(EntityDamageByEntityEvent event) {
        Entity e = event.getEntity();
        if (e instanceof EnderDragon) {
            if (event.getDamager() instanceof Player) {
                Player p = (Player) event.getDamager();
                dispatchPlayMusicFunctionAtEntity(e, p);
            } else {
                dispatchPlayMusicFunctionAtEntity(e, null);
            }
        }
    }

    // If the dragon targets a player (aggro), trigger the music function at the dragon for that player
    @EventHandler
    public void onDragonTarget(EntityTargetEvent event) {
        Entity e = event.getEntity();
        if (e instanceof EnderDragon && event.getTarget() instanceof Player) {
            Player p = (Player) event.getTarget();
            dispatchPlayMusicFunctionAtEntity(e, p);
        }
    }

    private void dispatchPlayMusicFunctionAtEntity(Entity entity, Player target) {
        String functionName = "vct:custom/ender_dragon_music";

        // We execute at the dragon's position and run the function there. The datapack function will
        // check for players within 500 blocks using its selector.
        Bukkit.getScheduler().runTask(plugin, () -> {
            String cmd;
            if (target != null) {
                // Execute as and at the dragon so the function can use @s (the player) if needed
                cmd = "execute as " + target.getName() + " at @e[type=ender_dragon,limit=1,sort=nearest] run function " + functionName;
            } else {
                cmd = "execute at @e[type=ender_dragon,limit=1,sort=nearest] run function " + functionName;
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        });
    }
}
