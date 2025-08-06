package vct.voiidstudios.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import vct.voiidstudios.api.Timer;
import vct.voiidstudios.api.events.TimerCreate;
import vct.voiidstudios.api.events.TimerFinish;
import vct.voiidstudios.api.events.TimerPause;
import vct.voiidstudios.api.events.TimerResume;

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
