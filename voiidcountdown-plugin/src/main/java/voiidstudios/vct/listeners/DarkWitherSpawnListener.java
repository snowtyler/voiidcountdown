package voiidstudios.vct.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Listens for Wither spawns with the "DarkWither" tag and applies a custom name.
 */
public class DarkWitherSpawnListener implements Listener {
    private static final String WITHER_TAG = "DarkWither";
    private static final String CUSTOM_NAME_JSON = "{\"text\":\"\",\"extra\":[{\"text\":\"W\",\"color\":\"#AC1C45\",\"bold\":true},{\"text\":\"I\",\"color\":\"#9E1751\",\"bold\":true},{\"text\":\"T\",\"color\":\"#91135D\",\"bold\":true},{\"text\":\"H\",\"color\":\"#830E69\",\"bold\":true},{\"text\":\"E\",\"color\":\"#750975\",\"bold\":true},{\"text\":\"R\",\"color\":\"#680581\",\"bold\":true},{\"text\":\" \"},{\"text\":\"S\",\"color\":\"#6D0192\",\"bold\":true},{\"text\":\"P\",\"color\":\"#800297\",\"bold\":true},{\"text\":\"A\",\"color\":\"#92039D\",\"bold\":true},{\"text\":\"W\",\"color\":\"#A504A2\",\"bold\":true},{\"text\":\"N\",\"color\":\"#B805A7\",\"bold\":true}]}";

    public DarkWitherSpawnListener(JavaPlugin plugin) {
        // Constructor parameter kept for consistency with other listeners in codebase
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWitherSpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        
        // Only process Wither entities
        if (!(entity instanceof Wither)) return;
        
        // Only process Withers with the DarkWither tag
        if (!entity.getScoreboardTags().contains(WITHER_TAG)) return;
        
        Wither wither = (Wither) entity;
        
        // Try to apply the custom name using Adventure API (Paper 1.16.5+)
        try {
            // Try Adventure API through reflection for compatibility
            Class<?> gsonComponentSerializerClass = Class.forName("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer");
            Object gsonSerializer = gsonComponentSerializerClass.getMethod("gson").invoke(null);
            
            // Deserialize JSON to Component
            Object component = gsonComponentSerializerClass
                    .getMethod("deserialize", String.class)
                    .invoke(gsonSerializer, CUSTOM_NAME_JSON);
            
            // Set the custom name using Paper's customName(Component) method
            wither.getClass()
                    .getMethod("customName", Class.forName("net.kyori.adventure.text.Component"))
                    .invoke(wither, component);
            
            // Make the name visible
            wither.setCustomNameVisible(true);
            
        } catch (Exception e) {
            // Fallback: use legacy string name if Adventure API is not available
            wither.setCustomName("§4§lWITHER SPAWN");
            wither.setCustomNameVisible(true);
        }
    }
}
