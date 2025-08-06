package vct.voiidstudios.managers;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import vct.voiidstudios.VoiidCountdownTimer;
import vct.voiidstudios.api.Timer;
import vct.voiidstudios.api.events.TimerFinish;
import vct.voiidstudios.utils.MessageUtils;

public class TimerManager {
    private static TimerManager instance;
    private Timer timer;

    private TimerManager() {}

    public static TimerManager getInstance() {
        if (instance == null) instance = new TimerManager();
        return instance;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public Timer getTimer() {
        return this.timer;
    }

    public void removeTimer() {
        if (this.timer != null) {
            this.timer.stop();
            this.timer = null;
        }
    }

    public void finishTimer() {
        if (this.timer != null) {
            TimerFinish event = new TimerFinish(this.timer);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    public void deleteTimer(CommandSender sender) {
        if (this.timer != null) {
            this.timer.stop();
            this.timer = null;
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix+VoiidCountdownTimer.getMainConfigManager().getTimerStop()));
        } else {
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix+VoiidCountdownTimer.getMainConfigManager().getTimerDontExists()));
        }
    }
}
