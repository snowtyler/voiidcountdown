package vct.voiidstudios.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import vct.voiidstudios.api.Timer;

public class TimerPause extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Timer timer;

    public TimerPause(Timer timer) {
        this.timer = timer;
    }

    public Timer getTimer() {
        return this.timer;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}