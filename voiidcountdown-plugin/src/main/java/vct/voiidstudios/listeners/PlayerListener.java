package vct.voiidstudios.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import vct.voiidstudios.VoiidCountdownTimer;
import vct.voiidstudios.utils.MessageUtils;

public class PlayerListener implements Listener {
    private final VoiidCountdownTimer plugin;

    public PlayerListener(VoiidCountdownTimer plugin){
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();

        // Update notification
        String latestVersion = plugin.getUpdateChecker().getLatestVersion();
        if(player.isOp() && latestVersion != null && !(plugin.version.equals(latestVersion))){
            player.sendMessage(MessageUtils.getColoredMessage(plugin.prefix+"&bAn update for Voiid Countdown Timer &e("+latestVersion+") &bis available."));
            player.sendMessage(MessageUtils.getColoredMessage("&bYou can download it at: &fhttps://modrinth.com/datapack/voiid-countdown-timer"));
        }
    }
}
