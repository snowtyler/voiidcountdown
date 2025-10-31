package voiidstudios.vct.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.managers.MessagesManager;

public class PlayerListener implements Listener {
    private final VoiidCountdownTimer plugin;
    

    public PlayerListener(VoiidCountdownTimer plugin){
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event){
        // Update notification
        Player player = event.getPlayer();
        String latestVersion = plugin.getUpdateChecker().getLatestVersion();
        if(player.isOp() && latestVersion != null && !(plugin.version.equals(latestVersion)) && VoiidCountdownTimer.getConfigsManager().getMainConfigManager().isUpdate_notification()){
            player.sendMessage(MessagesManager.getColoredMessage(VoiidCountdownTimer.prefix+"&bAn update for Voiid Countdown Timer &e("+latestVersion+") &bis available."));
            player.sendMessage(MessagesManager.getColoredMessage("&bYou can download it at: &ahttps://modrinth.com/datapack/voiid-countdown-timer"));
        }

        // No longer auto-giving prophecy books on join
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        // No longer auto-giving prophecy books on respawn
    }
}
