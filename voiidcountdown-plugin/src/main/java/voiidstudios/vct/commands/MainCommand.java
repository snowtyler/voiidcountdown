package voiidstudios.vct.commands;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.api.Timer;
import voiidstudios.vct.api.VCTEvent;
import voiidstudios.vct.configs.model.TimerConfig;
import voiidstudios.vct.managers.TimerManager;
import voiidstudios.vct.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getCommandNoPermissions()));
        }

        return true;
    }

    public void reload(CommandSender sender){
        VoiidCountdownTimer.getConfigsManager().reload();
        sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getCommandReload()));
        Timer.refreshTimerText();
    }

    public void set(CommandSender sender, String[] args){
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerSetError()));
            return;
        }

        String[] timeParts = args[1].split(":");
        if (timeParts.length != 3) {
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerSetFormatIncorrect()));
            return;
        }

        int hours, minutes, seconds;
        try {
            hours = Integer.parseInt(timeParts[0]);
            minutes = Integer.parseInt(timeParts[1]);
            seconds = Integer.parseInt(timeParts[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerSetFormatInvalid()));
            return;
        }

        if (hours < 0 || minutes < 0 || minutes > 59 || seconds < 0 || seconds > 59) {
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerSetFormatIncorrect()));
            return;
        }

        int totalSeconds = hours * 3600 + minutes * 60 + seconds;
        if (totalSeconds == 0) {
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerSetFormatOutRange()));
            return;
        }

        String usedTimerId = null;
        String text;
        String sound;
        BarColor color;

        if (args.length >= 3) {
            String wantedId = args[2];
            TimerConfig cfg = VoiidCountdownTimer.getConfigsManager().getTimerConfig(wantedId);
            if (cfg != null && cfg.isEnabled()) {
                usedTimerId = wantedId;
                text = cfg.getText();
                sound = cfg.getSound();
                color = cfg.getColor();
            } else {
                sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerDontExists()));
                return;
            }
        } else {
            TimerConfig defaultCfg = VoiidCountdownTimer.getConfigsManager().getTimerConfig("default");
            if (defaultCfg != null && defaultCfg.isEnabled()) {
                usedTimerId = "default";
                text = defaultCfg.getText();
                sound = defaultCfg.getSound();
                color = defaultCfg.getColor();
            } else {
                usedTimerId = null;
                text = "%HH%:%MM%:%SS%";
                sound = "UI_BUTTON_CLICK";
                color = BarColor.WHITE;
            }
        }

        TimerManager.getInstance().removeTimer();

        Timer timer = new Timer(
                totalSeconds,
                text,
                sound,
                color,
                usedTimerId
        );
        timer.start();
        TimerManager.getInstance().setTimer(timer);

        Bukkit.getPluginManager().callEvent(new VCTEvent(timer, VCTEvent.VCTEventType.CREATE, sender));

        sender.sendMessage(MessageUtils.getColoredMessage(
                VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerStart()
                        .replace("%HH%", String.format("%02d", hours))
                        .replace("%MM%", String.format("%02d", minutes))
                        .replace("%SS%", String.format("%02d", seconds))
        ));
    }

    public void pause(CommandSender sender){
        Timer timer = TimerManager.getInstance().getTimer();
        if (timer == null) {
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerDontExists()));
            return;
        }
        timer.pause();
        sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerPause()));

        Bukkit.getPluginManager().callEvent(new VCTEvent(timer, VCTEvent.VCTEventType.PAUSE, sender));
    }

    public void resume(CommandSender sender){
        Timer timer = TimerManager.getInstance().getTimer();
        if (timer == null) {
            sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerDontExists()));
            return;
        }
        timer.resume();
        sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerResume()));

        Bukkit.getPluginManager().callEvent(new VCTEvent(timer, VCTEvent.VCTEventType.RESUME, sender));
    }

    public void stop(CommandSender sender){
        TimerManager.getInstance().deleteTimer(sender);
    }

    public void modify(CommandSender sender, String[] args) {
        String timeToAdd;
        String[] timeParts;
        int addHours, addMinutes, addSeconds, totalSecondsToAdd;
        Timer timer;
        String timeToSet;
        String[] timePartsSet;
        int setHours, setMinutes, setSeconds, totalSecondsToSet;
        Timer timerSet;
        String timeToTake;
        String[] timePartsTake;
        int takeHours, takeMinutes, takeSeconds, totalSecondsToTake;
        Timer timerTake;

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
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerModifyAddError()));
                    return;
                }

                timeToAdd = args[2];
                timeParts = timeToAdd.split(":");

                if (timeParts.length != 3) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerSetFormatIncorrect()));
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
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerSetFormatInvalid()));
                    return;
                }
                if (addHours < 0 || addMinutes < 0 || addMinutes > 59 || addSeconds < 0 || addSeconds > 59) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerSetFormatIncorrect()));
                    return;
                }
                totalSecondsToAdd = addHours * 3600 + addMinutes * 60 + addSeconds;
                if (totalSecondsToAdd == 0) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerSetFormatOutRange()));
                    return;
                }

                if (TimerManager.getInstance().getTimer() == null) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerDontExists()));
                    return;
                }
                timer = TimerManager.getInstance().getTimer();
                timer.add(totalSecondsToAdd);
                Bukkit.getPluginManager().callEvent(new VCTEvent(timer, VCTEvent.VCTEventType.MODIFY, sender));
                sender.sendMessage(MessageUtils.getColoredMessage(
                        VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerModifyAdd()
                                .replace("%HH%", String.format("%02d", addHours))
                                .replace("%MM%", String.format("%02d", addMinutes))
                                .replace("%SS%", String.format("%02d", addSeconds))
                ));
                return;
            case "set":
                if (args.length < 3) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerModifySetError()));
                    return;
                }
                timeToSet = args[2];
                timePartsSet = timeToSet.split(":");
                if (timePartsSet.length != 3) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerSetFormatIncorrect()));
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
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerSetFormatInvalid()));
                    return;
                }
                if (setHours < 0 || setMinutes < 0 || setMinutes > 59 || setSeconds < 0 || setSeconds > 59) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerSetFormatIncorrect()));
                    return;
                }
                totalSecondsToSet = setHours * 3600 + setMinutes * 60 + setSeconds;
                if (TimerManager.getInstance().getTimer() == null) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerDontExists()));
                    return;
                }
                timerSet = TimerManager.getInstance().getTimer();
                timerSet.set(totalSecondsToSet);
                Bukkit.getPluginManager().callEvent(new VCTEvent(timerSet, VCTEvent.VCTEventType.MODIFY, sender));
                sender.sendMessage(MessageUtils.getColoredMessage(
                        VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerModifySet()
                                .replace("%HH%", String.format("%02d", setHours))
                                .replace("%MM%", String.format("%02d", setMinutes))
                                .replace("%SS%", String.format("%02d", setSeconds))
                ));
                return;
            case "take":
                if (args.length < 3) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerModifyTakeError()));
                    return;
                }
                timeToTake = args[2];
                timePartsTake = timeToTake.split(":");
                if (timePartsTake.length != 3) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerSetFormatIncorrect()));
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
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerSetFormatInvalid()));
                    return;
                }
                if (takeHours < 0 || takeMinutes < 0 || takeMinutes > 59 || takeSeconds < 0 || takeSeconds > 59) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerSetFormatIncorrect()));
                    return;
                }
                totalSecondsToTake = takeHours * 3600 + takeMinutes * 60 + takeSeconds;
                if (TimerManager.getInstance().getTimer() == null) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerDontExists()));
                    return;
                }
                timerTake = TimerManager.getInstance().getTimer();
                timerTake.take(totalSecondsToTake);
                Bukkit.getPluginManager().callEvent(new VCTEvent(timerTake, VCTEvent.VCTEventType.MODIFY, sender));
                sender.sendMessage(MessageUtils.getColoredMessage(
                        VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerModifyTake()
                                .replace("%HH%", String.format("%02d", takeHours))
                                .replace("%MM%", String.format("%02d", takeMinutes))
                                .replace("%SS%", String.format("%02d", takeSeconds))
                ));
                return;
            case "barcolor":
                if (args.length < 3) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerModifyBarcolorError()));
                    return;
                }
                String colorName = args[2].toUpperCase();

                if (TimerManager.getInstance().getTimer() == null) {
                    sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerDontExists()));
                    return;
                }

                try {
                    BarColor color = BarColor.valueOf(colorName);
                    Timer timerColor = TimerManager.getInstance().getTimer();
                    timerColor.setBossBarColor(color);

                    sender.sendMessage(MessageUtils.getColoredMessage(
                                    VoiidCountdownTimer.prefix+VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerModifyBarcolor())
                            .replace("%COLOR%", colorName)
                    );
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(MessageUtils.getColoredMessage(
                                    VoiidCountdownTimer.prefix+VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerModifyBarcolorInvalid())
                            .replace("%COLOR%", colorName)
                    );
                }
                return;
        }
        sender.sendMessage(MessageUtils.getColoredMessage(VoiidCountdownTimer.prefix + VoiidCountdownTimer.getConfigsManager().getMainConfigManager().getTimerModifyInvalid()));
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
                }else if(args[0].equalsIgnoreCase("set")){
                    subcommands.add("<HH:MM:SS>");
                }

                for(String c : subcommands) {
                    if(args[1].isEmpty() || c.startsWith(args[1].toLowerCase())) {
                        subcompletions.add(c);
                    }
                }
                return subcompletions;
            } else if (args.length == 3){
                List<String> subcompletions = new ArrayList<String>();
                List<String> subcommands = new ArrayList<String>();

                if(args[0].equalsIgnoreCase("modify")) {
                    if(args[1].equalsIgnoreCase("barcolor")){
                        subcommands.add("BLUE");subcommands.add("GREEN");
                        subcommands.add("PINK");subcommands.add("PURPLE");
                        subcommands.add("RED");subcommands.add("WHITE");
                        subcommands.add("YELLOW");
                    }else if(args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("take")){
                        subcommands.add("<HH:MM:SS>");
                    }
                } else if(args[0].equalsIgnoreCase("set")){
                    return getTimersCompletions(args, 2, true);
                }

                for(String c : subcommands) {
                    if(args[2].isEmpty() || c.startsWith(args[1].toLowerCase())) {
                        subcompletions.add(c);
                    }
                }
                return subcompletions;
            }
        }

        return null;
    }

    public List<String> getTimersCompletions(String[] args, int argTimerPos, boolean onlyEnabled) {
        List<String> completions = new ArrayList<>();

        String argTimer = args[argTimerPos].toLowerCase();

        Map<String, TimerConfig> timers = VoiidCountdownTimer.getConfigsManager().getAllTimerConfigs();
        if (timers != null) {
            for (Map.Entry<String, TimerConfig> entry : timers.entrySet()) {
                String id = entry.getKey();
                TimerConfig cfg = entry.getValue();

                if (cfg == null) continue;
                if (onlyEnabled && !cfg.isEnabled()) continue;

                if (argTimer.isEmpty() || id.toLowerCase().startsWith(argTimer)) {
                    completions.add(id);
                }
            }
        }

        return completions.isEmpty() ? null : completions;
    }

}