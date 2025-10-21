package voiidstudios.vct.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.entity.Player;
import voiidstudios.vct.managers.VisualBlockManager;

public class VisualsListener implements Listener {
    private final VisualBlockManager manager;

    public VisualsListener(VisualBlockManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        manager.reapplyToPlayer(p);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        manager.reapplyToChunk(e.getChunk());
    }
}
