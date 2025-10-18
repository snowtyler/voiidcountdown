package voiidstudios.vct.listeners.challenge;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import voiidstudios.vct.challenges.ChallengeManager;

public class ChallengeListener implements Listener {
    private final ChallengeManager challengeManager;

    public ChallengeListener(ChallengeManager challengeManager) {
        this.challengeManager = challengeManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        challengeManager.handleEntityKill(event.getEntityType(), killer);
    }
}
