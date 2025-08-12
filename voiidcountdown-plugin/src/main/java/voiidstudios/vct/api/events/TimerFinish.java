package voiidstudios.vct.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import voiidstudios.vct.api.Timer;

public class TimerFinish extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Timer timer;

    public TimerFinish(Timer timer) {
        this.timer = timer;
    }

    public Timer getTimer() {
        return this.timer;
    }

    public String getInitialTime() {
        return this.timer.getInitialTime();
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
