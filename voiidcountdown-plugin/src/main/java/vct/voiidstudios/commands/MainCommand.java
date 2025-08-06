package vct.voiidstudios.commands;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import vct.voiidstudios.VoiidCountdownTimer;
import vct.voiidstudios.api.Timer;
import vct.voiidstudios.api.events.TimerCreate;
import vct.voiidstudios.api.events.TimerPause;
import vct.voiidstudios.api.events.TimerResume;
import vct.voiidstudios.managers.TimerManager;
import vct.voiidstudios.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if(sender.isOp() || sender.hasPermission("voiidcountdowntimer.admin")) {
            if(args.length >= 1){
                if(args[0].equalsIgnoreCase("help")){
                    help(sender);
                }else if (args[0].equalsIgnoreCase("reload")){
                    reload(sender);
                }else if (args[0].equalsIgnoreCase("set")){
                    set(sender, args);
                }else if (args[0].equalsIgnoreCase("pause")){
                    pause(sender);
                }else if (args[0].equalsIgnoreCase("resume")){
                    resume(sender);
                }else if (args[0].equalsIgnoreCase("stop")){
                    stop(sender);
                }else if (args[0].equalsIgnoreCase("modify")){
                    modify(sender, args);
                }else{
                    help(sender);
                }
            }else{
                help(sender);
            }
        }else{
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getCommandNoPermissions()));
        }

        return true;
    }

    public void reload(CommandSender sender){
        VoiidCountdownTimer.getMainConfigManager().reloadConfig();
        sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getCommandReload()));
        Timer.refreshTimerText();
    }

    public void set(CommandSender sender, String[] args){
        if (args.length == 1) {
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerSetError()));
            return;
        }

        String[] timeParts = args[1].split(":");
        if (timeParts.length != 3) {
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerSetFormatIncorrect()));
            return;
        }

        int hours, minutes, seconds;
        try {
            hours = Integer.parseInt(timeParts[0]);
            minutes = Integer.parseInt(timeParts[1]);
            seconds = Integer.parseInt(timeParts[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerSetFormatInvalid()));
            return;
        }

        if (hours < 0 || minutes < 0 || minutes > 59 || seconds < 0 || seconds > 59) {
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerSetFormatIncorrect()));
            return;
        }

        int totalSeconds = hours * 3600 + minutes * 60 + seconds;
        if (totalSeconds == 0) {
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerSetFormatOutRange()));
            return;
        }

        TimerManager.getInstance().removeTimer();

        Timer timer = new Timer(
                totalSeconds,
                VoiidCountdownTimer.getMainConfigManager().getTimer_bossbar_text(),
                VoiidCountdownTimer.getMainConfigManager().getTimer_sound(),
                VoiidCountdownTimer.getMainConfigManager().getRefresh_ticks()
        );
        timer.start();

        TimerManager.getInstance().setTimer(timer);

        TimerCreate timerCreate = new TimerCreate(timer);
        Bukkit.getPluginManager().callEvent(timerCreate);

        sender.sendMessage(MessageUtils.getColoredMessage(
                VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerStart()
                        .replace("%HH%", String.format("%02d", hours))
                        .replace("%MM%", String.format("%02d", minutes))
                        .replace("%SS%", String.format("%02d", seconds))
        ));
    }

    public void pause(CommandSender sender){
        Timer timer = TimerManager.getInstance().getTimer();
        if (timer == null) {
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerDontExists()));
            return;
        }
        timer.pause();
        sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerPause()));
        TimerPause timerPause = new TimerPause(timer);
        Bukkit.getPluginManager().callEvent(timerPause);
    }

    public void resume(CommandSender sender){
        Timer timer = TimerManager.getInstance().getTimer();
        if (timer == null) {
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerDontExists()));
            return;
        }
        timer.resume();
        sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerResume()));
        TimerResume timerResume = new TimerResume(timer);
        Bukkit.getPluginManager().callEvent(timerResume);
    }

    public void stop(CommandSender sender){
        TimerManager.getInstance().deleteTimer(sender);
    }

    public void modify(CommandSender sender, String[] args) {
        String timeToAdd;
        String[] timeParts;
        int addHours, addMinutes, addSeconds, totalSecondsToAdd;
        Timer timer;
        TimerCreate timerCreate;
        String timeToSet;
        String[] timePartsSet;
        int setHours, setMinutes, setSeconds, totalSecondsToSet;
        Timer timerSet;
        TimerCreate timerCreateSet;
        String timeToTake;
        String[] timePartsTake;
        int takeHours, takeMinutes, takeSeconds, totalSecondsToTake;
        Timer timerTake;
        TimerCreate timerCreateTake;

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix +"&7Modifiers for the timer"));
            sender.sendMessage(MessageUtils.getColoredMessage("&6> &eadd &7- Add time to the timer."));
            sender.sendMessage(MessageUtils.getColoredMessage("&6> &eset &7- Set time to the timer."));
            sender.sendMessage(MessageUtils.getColoredMessage("&6> &etake &7- Take time to the timer."));
            sender.sendMessage(MessageUtils.getColoredMessage("&6> &ebarcolor &7- Change the color of the bossbar."));

            return;
        }

        String modifier = args[1].toLowerCase();
        switch (modifier) {
            case "add":
                if (args.length < 3) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerModifyAddError()));
                    return;
                }

                timeToAdd = args[2];
                timeParts = timeToAdd.split(":");

                if (timeParts.length != 3) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerSetFormatIncorrect()));
                    return;
                }
                addHours = 0;
                addMinutes = 0;
                addSeconds = 0;
                try {
                    addHours = Integer.parseInt(timeParts[0]);
                    addMinutes = Integer.parseInt(timeParts[1]);
                    addSeconds = Integer.parseInt(timeParts[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerSetFormatInvalid()));
                    return;
                }
                if (addHours < 0 || addMinutes < 0 || addMinutes > 59 || addSeconds < 0 || addSeconds > 59) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerSetFormatIncorrect()));
                    return;
                }
                totalSecondsToAdd = addHours * 3600 + addMinutes * 60 + addSeconds;
                if (totalSecondsToAdd == 0) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerSetFormatOutRange()));
                    return;
                }

                if (TimerManager.getInstance().getTimer() == null) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerDontExists()));
                    return;
                }
                timer = TimerManager.getInstance().getTimer();
                timer.add(totalSecondsToAdd);
                timerCreate = new TimerCreate(timer);
                Bukkit.getPluginManager().callEvent(timerCreate);
                sender.sendMessage(MessageUtils.getColoredMessage(
                        VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerModifyAdd()
                                .replace("%HH%", String.format("%02d", addHours))
                                .replace("%MM%", String.format("%02d", addMinutes))
                                .replace("%SS%", String.format("%02d", addSeconds))
                ));
                return;
            case "set":
                if (args.length < 3) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerModifySetError()));
                    return;
                }
                timeToSet = args[2];
                timePartsSet = timeToSet.split(":");
                if (timePartsSet.length != 3) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerSetFormatIncorrect()));
                    return;
                }
                setHours = 0;
                setMinutes = 0;
                setSeconds = 0;
                try {
                    setHours = Integer.parseInt(timePartsSet[0]);
                    setMinutes = Integer.parseInt(timePartsSet[1]);
                    setSeconds = Integer.parseInt(timePartsSet[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerSetFormatInvalid()));
                    return;
                }
                if (setHours < 0 || setMinutes < 0 || setMinutes > 59 || setSeconds < 0 || setSeconds > 59) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerSetFormatIncorrect()));
                    return;
                }
                totalSecondsToSet = setHours * 3600 + setMinutes * 60 + setSeconds;
                if (TimerManager.getInstance().getTimer() == null) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerDontExists()));
                    return;
                }
                timerSet = TimerManager.getInstance().getTimer();
                timerSet.set(totalSecondsToSet);
                timerCreateSet = new TimerCreate(timerSet);
                Bukkit.getPluginManager().callEvent(timerCreateSet);
                sender.sendMessage(MessageUtils.getColoredMessage(
                        VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerModifySet()
                                .replace("%HH%", String.format("%02d", setHours))
                                .replace("%MM%", String.format("%02d", setMinutes))
                                .replace("%SS%", String.format("%02d", setSeconds))
                ));
                return;
            case "take":
                if (args.length < 3) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerModifyTakeError()));
                    return;
                }
                timeToTake = args[2];
                timePartsTake = timeToTake.split(":");
                if (timePartsTake.length != 3) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerSetFormatIncorrect()));
                    return;
                }
                takeHours = 0;
                takeMinutes = 0;
                takeSeconds = 0;
                try {
                    takeHours = Integer.parseInt(timePartsTake[0]);
                    takeMinutes = Integer.parseInt(timePartsTake[1]);
                    takeSeconds = Integer.parseInt(timePartsTake[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerSetFormatInvalid()));
                    return;
                }
                if (takeHours < 0 || takeMinutes < 0 || takeMinutes > 59 || takeSeconds < 0 || takeSeconds > 59) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerSetFormatIncorrect()));
                    return;
                }
                totalSecondsToTake = takeHours * 3600 + takeMinutes * 60 + takeSeconds;
                if (TimerManager.getInstance().getTimer() == null) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerDontExists()));
                    return;
                }
                timerTake = TimerManager.getInstance().getTimer();
                timerTake.take(totalSecondsToTake);
                timerCreateTake = new TimerCreate(timerTake);
                Bukkit.getPluginManager().callEvent(timerCreateTake);
                sender.sendMessage(MessageUtils.getColoredMessage(
                        VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerModifyTake()
                                .replace("%HH%", String.format("%02d", takeHours))
                                .replace("%MM%", String.format("%02d", takeMinutes))
                                .replace("%SS%", String.format("%02d", takeSeconds))
                ));
                return;
            case "barcolor":
                if (args.length < 3) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerModifyBarcolorError()));
                    return;
                }
                String colorName = args[2].toUpperCase();

                if (TimerManager.getInstance().getTimer() == null) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerDontExists()));
                    return;
                }

                try {
                    BarColor color = BarColor.valueOf(colorName);
                    Timer timerColor = TimerManager.getInstance().getTimer();
                    timerColor.setBossBarColor(color);

                    sender.sendMessage(MessageUtils.getColoredMessage(
                                    VoiidCountdownTimer.prefix+VoiidCountdownTimer.getMainConfigManager().getTimerModifyBarcolor())
                            .replace("%COLOR%", colorName)
                    );
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(MessageUtils.getColoredMessage(
                                    VoiidCountdownTimer.prefix+VoiidCountdownTimer.getMainConfigManager().getTimerModifyBarcolorInvalid())
                            .replace("%COLOR%", colorName)
                    );
                }
                return;
        }
        sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getMainConfigManager().getTimerModifyInvalid()));
    }

    public void help(CommandSender sender){
        sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix +"&7Running &dVoiid Countdown Timer &ev"+VoiidCountdownTimer.getInstance().getDescription().getVersion()));
        sender.sendMessage(MessageUtils.getColoredMessage("&5> &6/vct help &7- Shows this message."));
        sender.sendMessage(MessageUtils.getColoredMessage("&5> &6/vct reload &7- Reloads the config."));
        sender.sendMessage(MessageUtils.getColoredMessage("&5> &6/vct set &e<HH:MM:SS> &7- Set the timer."));
        sender.sendMessage(MessageUtils.getColoredMessage("&5> &6/vct pause &7- Pause the timer."));
        sender.sendMessage(MessageUtils.getColoredMessage("&5> &6/vct resume &7- Resume the timer."));
        sender.sendMessage(MessageUtils.getColoredMessage("&5> &6/vct stop &7- Stop the timer."));
        sender.sendMessage(MessageUtils.getColoredMessage("&5> &6/vct modify &e<modifier> &7- Modify the timer."));
    }

    public List<String> onTabComplete(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args){
        if(sender.isOp() || sender.hasPermission("voiidcountdowntimer.admin")){
            if(args.length == 1){
                List<String> completions = new ArrayList<String>();
                List<String> commands = new ArrayList<String>();
                commands.add("help");commands.add("reload");
                commands.add("set");commands.add("pause");
                commands.add("resume");commands.add("stop");
                commands.add("modify");
                for(String c : commands) {
                    if(args[0].isEmpty() || c.startsWith(args[0].toLowerCase())) {
                        completions.add(c);
                    }
                }
                return completions;
            } else if (args.length == 2){
                List<String> subcompletions = new ArrayList<String>();
                List<String> subcommands = new ArrayList<String>();

                if(args[0].equalsIgnoreCase("modify")) {
                    subcommands.add("add");subcommands.add("set");
                    subcommands.add("take");subcommands.add("barcolor");
                }

                for(String c : subcommands) {
                    if(args[1].isEmpty() || c.startsWith(args[1].toLowerCase())) {
                        subcompletions.add(c);
                    }
                }
                return subcompletions;
            }
        }

        return null;
    }
}