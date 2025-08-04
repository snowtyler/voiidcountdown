package vct.voiidstudios.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import vct.voiidstudios.api.Timer;
import vct.voiidstudios.api.events.TimerCreateEvent;
import vct.voiidstudios.api.events.TimerFinishEvent;
import vct.voiidstudios.api.events.TimerPauseEvent;
import vct.voiidstudios.api.events.TimerResumeEvent;

public class EventListener implements Listener {
    @EventHandler
    public void onTimerCreate(TimerCreateEvent event) {
        Timer timer = event.getTimer();
    }

    @EventHandler
    public void onTimerFinish(TimerFinishEvent event) {
        Timer timer = event.getTimer();
    }

    @EventHandler
    public void onTimerPause(TimerPauseEvent event) {
        Timer timer = event.getTimer();
    }

    @EventHandler
    public void onTimerResume(TimerResumeEvent event) {
        Timer timer = event.getTimer();
    }
}
