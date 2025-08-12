package voiidstudios.vct.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import voiidstudios.vct.api.Timer;

public class TimerPause extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Timer timer;

    public TimerPause(Timer timer) {
        this.timer = timer;
    }

    public Timer getTimer() {
        return this.timer;
    }

    public String getTimeLeft() {
        return this.timer.getTimeLeft();
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}