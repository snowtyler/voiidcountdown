package voiidstudios.vct.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import voiidstudios.vct.api.Timer;
import voiidstudios.vct.api.events.TimerCreate;
import voiidstudios.vct.api.events.TimerFinish;
import voiidstudios.vct.api.events.TimerPause;
import voiidstudios.vct.api.events.TimerResume;

public class EventListener implements Listener {
    @EventHandler
    public void onTimerCreate(TimerCreate event) {
        Timer timer = event.getTimer();
    }

    @EventHandler
    public void onTimerFinish(TimerFinish event) {
        Timer timer = event.getTimer();
    }

    @EventHandler
    public void onTimerPause(TimerPause event) {
        Timer timer = event.getTimer();
    }

    @EventHandler
    public void onTimerResume(TimerResume event) {
        Timer timer = event.getTimer();
    }
}
