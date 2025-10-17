package voiidstudios.vct.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.api.Timer;
import voiidstudios.vct.api.VCTActions;
import voiidstudios.vct.api.VCTEvent;
import voiidstudios.vct.configs.model.TimerConfig;
import voiidstudios.vct.managers.MessagesManager;
import voiidstudios.vct.managers.TimerManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainCommand implements CommandExecutor, TabCompleter {
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        MessagesManager msgManager = VoiidCountdownTimer.getMessagesManager();

        if(sender.isOp() || sender.hasPermission("voiidcountdowntimer.admin")) {
            if(args.length >= 1){
                if(args[0].equalsIgnoreCase("help")){
                    help(sender);
                }else if (args[0].equalsIgnoreCase("reload")){
                    reload(sender, msgManager);
                }else if (args[0].equalsIgnoreCase("set")){
                    set(sender, args, msgManager);
                }else if (args[0].equalsIgnoreCase("pause")){
                    pause(sender, msgManager);
                }else if (args[0].equalsIgnoreCase("resume")){
                    resume(sender, msgManager);
                }else if (args[0].equalsIgnoreCase("stop")){
                    stop(sender);
                }else if (args[0].equalsIgnoreCase("modify")){
                    modify(sender, args, msgManager);
                }else{
                    help(sender);
                }
            }else{
                help(sender);
            }
        }else{
            msgManager.sendConfigMessage(sender, "Messages.commandNoPermissions", true, null);
        }

        return true;
    }

    public void reload(CommandSender sender, MessagesManager msgManager){
        VoiidCountdownTimer.getConfigsManager().reload();
        msgManager.sendConfigMessage(sender, "Messages.commandReload", true, null);
        Timer.refreshTimerText();
    }

    public void set(CommandSender sender, String[] args, MessagesManager msgManager){
        if (args.length < 2) {
            msgManager.sendConfigMessage(sender, "Messages.timerSetError", true, null);
            return;
        }

        String timeHHMMSS = args[1];
        String timerId = (args.length >= 3) ? args[2] : null;

        Timer timer = VCTActions.createTimer(timeHHMMSS, timerId, sender);
        if (timer == null) {
            msgManager.sendConfigMessage(sender, "Messages.timerSetFormatIncorrect", true, null);
            return;
        }

        Map<String, String> repl = new HashMap<>();
        repl.put("%HH%", String.format("%02d", Integer.parseInt(timer.getTimeLeftHH())));
        repl.put("%MM%", String.format("%02d", Integer.parseInt(timer.getTimeLeftMM())));
        repl.put("%SS%", String.format("%02d", Integer.parseInt(timer.getTimeLeftSS())));

        msgManager.sendConfigMessage(sender, "Messages.timerStart", true, repl);
    }

    public void pause(CommandSender sender, MessagesManager msgManager){
        Timer timer = TimerManager.getInstance().getTimer();
        if (timer == null) {
            msgManager.sendConfigMessage(sender, "Messages.timerDontExists", true, null);
            return;
        }
        timer.pause();
        msgManager.sendConfigMessage(sender, "Messages.timerPause", true, null);

        Bukkit.getPluginManager().callEvent(new VCTEvent(timer, VCTEvent.VCTEventType.PAUSE, sender));
    }

    public void resume(CommandSender sender, MessagesManager msgManager){
        Timer timer = TimerManager.getInstance().getTimer();
        if (timer == null) {
            msgManager.sendConfigMessage(sender, "Messages.timerDontExists", true, null);
            return;
        }
        timer.resume();
        msgManager.sendConfigMessage(sender, "Messages.timerResume", true, null);

        Bukkit.getPluginManager().callEvent(new VCTEvent(timer, VCTEvent.VCTEventType.RESUME, sender));
    }

    public void stop(CommandSender sender){
        TimerManager.getInstance().deleteTimer(sender);
    }

    public void modify(CommandSender sender, String[] args, MessagesManager msgManager) {
        java.util.List<String> parts;
        int addHours, addMinutes, addSeconds, totalSecondsToAdd;
        Timer timer;
        int setHours, setMinutes, setSeconds, totalSecondsToSet;
        int takeHours, takeMinutes, takeSeconds, totalSecondsToTake;

        if (args.length < 2) {
            sender.sendMessage(MessagesManager.getColoredMessage(VoiidCountdownTimer.prefix +"&7Modifiers for the timer"));
            sender.sendMessage(MessagesManager.getColoredMessage("&6> &eadd &7- Add time to the timer."));
            sender.sendMessage(MessagesManager.getColoredMessage("&6> &eset &7- Set time to the timer."));
            sender.sendMessage(MessagesManager.getColoredMessage("&6> &etake &7- Take time to the timer."));
            sender.sendMessage(MessagesManager.getColoredMessage("&6> &ebossbar_color &7- Change the color of the bossbar."));
            sender.sendMessage(MessagesManager.getColoredMessage("&6> &ebossbar_style &7- Change the segments style of the bossbar."));
            sender.sendMessage(MessagesManager.getColoredMessage("&6> &esound &7- Change the sound that plays each time a second is lowered."));
            sender.sendMessage(MessagesManager.getColoredMessage("&6> &esound_enable &7- Toggle whether the sound should be played or not."));
            sender.sendMessage(MessagesManager.getColoredMessage("&6> &esound_volume &7- Change the volume of the sound being played."));
            sender.sendMessage(MessagesManager.getColoredMessage("&6> &esound_pitch &7- Change the pitch of the sound being played."));
            sender.sendMessage(MessagesManager.getColoredMessage("&6> &etext &7- Change the text of the boss bar."));
            return;
        }

        timer = TimerManager.getInstance().getTimer();
        if (timer == null) {
            msgManager.sendConfigMessage(sender, "Messages.timerDontExists", true, null);
            return;
        }

        String modifier = args[1].toLowerCase();
        switch (modifier) {
            case "add":
                if (args.length < 3) {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifyAddError", true, null);
                    return;
                }

                totalSecondsToAdd = VCTActions.helper_parseTimeToSeconds(args[2]);

                addHours = 0;
                addMinutes = 0;
                addSeconds = 0;

                addHours = totalSecondsToAdd / 3600;
                addMinutes = (totalSecondsToAdd % 3600) / 60;
                addSeconds = totalSecondsToAdd % 60;

                if (addHours < 0 || addMinutes < 0 || addMinutes > 59 || addSeconds < 0 || addSeconds > 59) {
                    msgManager.sendConfigMessage(sender, "Messages.timerSetFormatIncorrect", true, null);
                    return;
                }

                if (totalSecondsToAdd == 0) {
                    msgManager.sendConfigMessage(sender, "Messages.timerSetFormatOutRange", true, null);
                    return;
                }

                boolean addSuccess = VCTActions.modifyTimer("add", args[2], sender);
                if (!addSuccess) {
                    msgManager.sendConfigMessage(sender, "Messages.timerDontExists", true, null);
                    return;
                }

                Map<String, String> addRepl = new HashMap<>();
                addRepl.put("%HH%", String.format("%02d", addHours));
                addRepl.put("%MM%", String.format("%02d", addMinutes));
                addRepl.put("%SS%", String.format("%02d", addSeconds));

                msgManager.sendConfigMessage(sender, "Messages.timerModifyAdd", true, addRepl);
                return;
            case "set":
                if (args.length < 3) {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifyTakeError", true, null);
                    return;
                }

                totalSecondsToSet = VCTActions.helper_parseTimeToSeconds(args[2]);

                setHours = 0;
                setMinutes = 0;
                setSeconds = 0;

                setHours = totalSecondsToSet / 3600;
                setMinutes = (totalSecondsToSet % 3600) / 60;
                setSeconds = totalSecondsToSet % 60;

                if (setHours < 0 || setMinutes < 0 || setMinutes > 59 || setSeconds < 0 || setSeconds > 59) {
                    msgManager.sendConfigMessage(sender, "Messages.timerSetFormatIncorrect", true, null);
                    return;
                }

                boolean setSuccess = VCTActions.modifyTimer("set", args[2], sender);
                if (!setSuccess) {
                    msgManager.sendConfigMessage(sender, "Messages.timerDontExists", true, null);
                    return;
                }

                Map<String, String> setRepl = new HashMap<>();
                setRepl.put("%HH%", String.format("%02d", setHours));
                setRepl.put("%MM%", String.format("%02d", setMinutes));
                setRepl.put("%SS%", String.format("%02d", setSeconds));

                msgManager.sendConfigMessage(sender, "Messages.timerModifySet", true, setRepl);
                return;
            case "take":
                if (args.length < 3) {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifyTakeError", true, null);
                    return;
                }

                totalSecondsToTake = VCTActions.helper_parseTimeToSeconds(args[2]);

                takeHours = 0;
                takeMinutes = 0;
                takeSeconds = 0;

                takeHours = totalSecondsToTake / 3600;
                takeMinutes = (totalSecondsToTake % 3600) / 60;
                takeSeconds = totalSecondsToTake % 60;

                if (takeHours < 0 || takeMinutes < 0 || takeMinutes > 59 || takeSeconds < 0 || takeSeconds > 59) {
                    msgManager.sendConfigMessage(sender, "Messages.timerSetFormatIncorrect", true, null);
                    return;
                }

                boolean takeSuccess = VCTActions.modifyTimer("take", args[2], sender);
                if (!takeSuccess) {
                    msgManager.sendConfigMessage(sender, "Messages.timerDontExists", true, null);
                    return;
                }

                Map<String, String> takeRepl = new HashMap<>();
                takeRepl.put("%HH%", String.format("%02d", takeHours));
                takeRepl.put("%MM%", String.format("%02d", takeMinutes));
                takeRepl.put("%SS%", String.format("%02d", takeSeconds));

                msgManager.sendConfigMessage(sender, "Messages.timerModifyTake", true, takeRepl);
                return;
            case "bossbar_color":
                if (args.length < 3) {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifyBarcolorError", true, null);
                    return;
                }
                String colorName = args[2].toUpperCase();

                boolean bcSuccess = VCTActions.modifyTimer("bossbar_color", colorName, sender);

                if (bcSuccess) {
                    Map<String, String> barcolorRepl = new HashMap<>();
                    barcolorRepl.put("%TIMER%", timer.getTimerId());
                    barcolorRepl.put("%COLOR%", colorName);

                    msgManager.sendConfigMessage(sender, "Messages.timerModifyBarcolor", true, barcolorRepl);
                } else {
                    Map<String, String> barcolorInvRepl = new HashMap<>();
                    barcolorInvRepl.put("%COLOR%", colorName);

                    msgManager.sendConfigMessage(sender, "Messages.timerModifyBarcolorInvalid", true, barcolorInvRepl);
                }
                return;
            case "bossbar_style":
                if (args.length < 3) {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifyBarstyleError", true, null);
                    return;
                }
                String styleName = args[2].toUpperCase();

                boolean bsSuccess = VCTActions.modifyTimer("bossbar_style", styleName, sender);

                if (bsSuccess) {
                    Map<String, String> barstyleRepl = new HashMap<>();
                    barstyleRepl.put("%TIMER%", timer.getTimerId());
                    barstyleRepl.put("%STYLE%", styleName);

                    msgManager.sendConfigMessage(sender, "Messages.timerModifyBarstyle", true, barstyleRepl);
                } else {
                    Map<String, String> barstyleInvRepl = new HashMap<>();
                    barstyleInvRepl.put("%STYLE%", styleName);

                    msgManager.sendConfigMessage(sender, "Messages.timerModifyBarstyleInvalid", true, barstyleInvRepl);
                }
                return;
            case "sound":
                if (args.length < 3) {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifySoundError", true, null);
                    return;
                }

                parts = new java.util.ArrayList<>();
                for (int i = 2; i < args.length; i++) {
                    parts.add(args[i]);
                }
                String rawSound = String.join(" ", parts).trim();

                if (rawSound.startsWith("\"") && rawSound.endsWith("\"") && rawSound.length() >= 2) {
                    rawSound = rawSound.substring(1, rawSound.length() - 1).trim();
                } else {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifySoundRequireQuotes", true, null);
                    return;
                }

                if (rawSound.isEmpty()) return;

                boolean isVanillaSound = false;
                try {
                    String enumName = rawSound.toUpperCase(java.util.Locale.ROOT).replace(':', '_');
                    org.bukkit.Sound.valueOf(enumName);
                    isVanillaSound = true;
                } catch (IllegalArgumentException ignored) {}

                boolean soundSuccess = VCTActions.modifyTimer("sound", rawSound, sender);

                if (soundSuccess) {
                    Map<String, String> soundRepl = new HashMap<>();
                    soundRepl.put("%TIMER%", timer.getTimerId());
                    soundRepl.put("%SOUND%", rawSound);
                    soundRepl.put("%TYPE%", isVanillaSound ? "vanilla" : "custom");

                    msgManager.sendConfigMessage(sender, "Messages.timerModifySound", true, soundRepl);
                } else {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifySoundError", true, null);
                }
                return;
            case "sound_enable":
                if (args.length < 3) {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifySoundenableError", true, null);
                    return;
                }

                String value = args[2].toLowerCase();

                boolean seSuccess = VCTActions.modifyTimer("sound_enable", value, sender);

                if (seSuccess) {
                    Map<String, String> soundenableRepl = new HashMap<>();
                    soundenableRepl.put("%TIMER%", timer.getTimerId());
                    soundenableRepl.put("%SOUNDENABLE%", value);

                    msgManager.sendConfigMessage(sender, "Messages.timerModifySoundenable", true, soundenableRepl);
                } else {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifySoundenableInvalid", true, null);
                }
                return;
            case "sound_volume":
                if (args.length < 3) {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifySoundvolumeError", true, null);
                    return;
                }

                float newVolume;
                try {
                    newVolume = Float.parseFloat(args[2]);
                } catch (NumberFormatException e) {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifySoundvolumeInvalid", true, null);
                    return;
                }

                if (newVolume < 0.1f || newVolume > 2.0f) {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifySoundvolumeOutRange", true, null);
                    return;
                }

                boolean svSuccess = VCTActions.modifyTimer("sound_volume", String.valueOf(newVolume), sender);

                if (svSuccess) {
                    Map<String, String> repl = new HashMap<>();
                    repl.put("%TIMER%", timer.getTimerId());
                    repl.put("%VOLUME%", String.valueOf(newVolume));
                    msgManager.sendConfigMessage(sender, "Messages.timerModifySoundvolume", true, repl);
                }
                return;
            case "sound_pitch":
                if (args.length < 3) {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifySoundpitchError", true, null);
                    return;
                }

                float newPitch;
                try {
                    newPitch = Float.parseFloat(args[2]);
                } catch (NumberFormatException e) {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifySoundpitchInvalid", true, null);
                    return;
                }

                if (newPitch < 0.1f || newPitch > 2.0f) {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifySoundpitchOutRange", true, null);
                    return;
                }

                boolean spSuccess = VCTActions.modifyTimer("sound_pitch", String.valueOf(newPitch), sender);

                if (spSuccess) {
                    Map<String, String> repl = new HashMap<>();
                    repl.put("%TIMER%", timer.getTimerId());
                    repl.put("%PITCH%", String.valueOf(newPitch));
                    msgManager.sendConfigMessage(sender, "Messages.timerModifySoundpitch", true, repl);
                }
                return;
            case "text":
                if (args.length < 3) {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifyTextError", true, null);
                    return;
                }

                parts = new java.util.ArrayList<>();
                for (int i = 2; i < args.length; i++) {
                    parts.add(args[i]);
                }
                String rawText = String.join(" ", parts).trim();

                if (rawText.startsWith("\"") && rawText.endsWith("\"") && rawText.length() >= 2) {
                    rawText = rawText.substring(1, rawText.length() - 1);
                } else {
                    msgManager.sendConfigMessage(sender, "Messages.timerModifyTextRequireQuotes", true, null);
                    return;
                }

                if (rawText.isEmpty()) return;

                boolean textSuccess = VCTActions.modifyTimer("text", rawText, sender);

                if (textSuccess) {
                    Map<String, String> repl = new HashMap<>();
                    repl.put("%TIMER%", timer.getTimerId());
                    repl.put("%TEXT%", rawText);
                    msgManager.sendConfigMessage(sender, "Messages.timerModifyText", true, repl);
                }
                return;
        }
        msgManager.sendConfigMessage(sender, "Messages.timerModifyInvalid", true, null);
    }
    

    public void help(CommandSender sender){
        sender.sendMessage(MessagesManager.getColoredMessage(VoiidCountdownTimer.prefix +"&7Running &dVoiid Countdown Timer &ev"+VoiidCountdownTimer.getInstance().getDescription().getVersion()));
        sender.sendMessage(MessagesManager.getColoredMessage("&5> &6/vct help &7- Shows this message."));
        sender.sendMessage(MessagesManager.getColoredMessage("&5> &6/vct reload &7- Reloads the config."));
        sender.sendMessage(MessagesManager.getColoredMessage("&5> &6/vct set &e<HHH:MM:SS> &7- Set the timer (hours can be 1-3 digits, max 999:59:59)."));
        sender.sendMessage(MessagesManager.getColoredMessage("&5> &6/vct pause &7- Pause the timer."));
        sender.sendMessage(MessagesManager.getColoredMessage("&5> &6/vct resume &7- Resume the timer."));
        sender.sendMessage(MessagesManager.getColoredMessage("&5> &6/vct stop &7- Stop the timer."));
        sender.sendMessage(MessagesManager.getColoredMessage("&5> &6/vct modify &e<modifier> &7- Modify the timer."));
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
                    subcommands.add("take");subcommands.add("bossbar_color");
                    subcommands.add("bossbar_style");subcommands.add("sound");
                    subcommands.add("sound_enable");subcommands.add("sound_volume");
                    subcommands.add("sound_pitch");subcommands.add("text");
                }else if(args[0].equalsIgnoreCase("set")){
                    subcommands.add("<HHH:MM:SS>");
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
                    if(args[1].equalsIgnoreCase("bossbar_color")){
                        subcommands.add("BLUE");subcommands.add("GREEN");
                        subcommands.add("PINK");subcommands.add("PURPLE");
                        subcommands.add("RED");subcommands.add("WHITE");
                        subcommands.add("YELLOW");
                    }else if(args[1].equalsIgnoreCase("bossbar_style")){
                        subcommands.add("SOLID");subcommands.add("SEGMENTED_6");
                        subcommands.add("SEGMENTED_10");subcommands.add("SEGMENTED_12");
                        subcommands.add("SEGMENTED_20");
                    }else if(args[1].equalsIgnoreCase("sound")){
                        subcommands.add("<\"sound in quotes\">");
                    }else if(args[1].equalsIgnoreCase("sound_enable")){
                        subcommands.add("true");subcommands.add("false");
                    }else if(args[1].equalsIgnoreCase("sound_volume") || args[1].equalsIgnoreCase("sound_pitch")){
                        subcommands.add("<0.1 - 2.0>");
                    }else if(args[1].equalsIgnoreCase("text")){
                        subcommands.add("<\"text in quotes\">");
                    }else if(args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("take")){
                        subcommands.add("<HHH:MM:SS>");
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