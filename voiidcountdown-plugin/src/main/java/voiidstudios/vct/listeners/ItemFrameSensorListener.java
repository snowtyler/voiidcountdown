package voiidstudios.vct.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.managers.ItemFrameSensorManager;

public class ItemFrameSensorListener implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        ItemFrameSensorManager manager = VoiidCountdownTimer.getItemFrameSensorManager();
        if (manager != null) manager.handleJoin(e.getPlayer());
    }
}
