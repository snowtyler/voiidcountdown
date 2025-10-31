package voiidstudios.vct.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.configs.FreezeConfigManager;
import voiidstudios.vct.configs.MainConfigManager;
import voiidstudios.vct.utils.SoundResolver;

public class FreezeManager {
    private final VoiidCountdownTimer plugin;
    private final FreezeConfigManager freezeConfigManager;
    private final MainConfigManager mainConfigManager;
    private final MessagesManager messagesManager;
    
    // Core state
    private boolean frozen;
    private boolean mobsFrozen;
    private String currentState; // "success" or "fail" or null
    private boolean inPostSequence; // true if sequence completed and we're in post-sequence state
    
    // Core settings
    private boolean killMobsOnFreeze;
    private boolean preventMobSpawn;
    private boolean muteChat;
    private boolean silentNotifications;
    private long notifyCooldownMs;
    private final Map<UUID, Long> lastNotification = new HashMap<>();
    
    // Mob management
    private final Map<UUID, Boolean> mobAiSnapshot = new HashMap<>();
    
    // Time management
    private boolean lockMidnight;
    private final Map<World, Boolean> prevDaylight = new HashMap<>();
    private final Map<World, Long> prevWorldTime = new HashMap<>();
    private BukkitTask timeTask;
    
    // State configurations
    private StateConfig successConfig;
    private StateConfig failConfig;
    
    // Music tasks
    private BukkitTask musicTask;
    
    // Sequence execution
    private final List<BukkitTask> sequenceTasks = new ArrayList<>();
    
    // Persistence
    private File freezeStateFile;

    public enum FreezeVariant { DEFAULT, SUCCESS, FAIL }
    
    private static class StateConfig {
        // Initial sound
        boolean initialSoundEnabled;
        String initialSoundSound;
        float initialSoundVolume;
        float initialSoundPitch;
        int initialSoundLengthTicks;
        
        // Music
        boolean musicEnabled;
        String musicSound;
        float musicVolume;
        float musicPitch;
        int musicLoopTicks;
        
        // Sequence
        List<SequenceStep> sequence = new ArrayList<>();
        
        // Post-sequence
        PostSequenceConfig postSequence;
    }
    
    private static class SequenceStep {
        String title;
        String subtitle;
        int fadeInTicks;
        int stayTicks;
        int fadeOutTicks;
        List<String> commands;
        
        SequenceStep(String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks, List<String> commands) {
            this.title = title;
            this.subtitle = subtitle;
            this.fadeInTicks = Math.max(0, fadeInTicks);
            this.stayTicks = Math.max(1, stayTicks);
            this.fadeOutTicks = Math.max(0, fadeOutTicks);
            this.commands = commands != null ? commands : new ArrayList<>();
        }
    }
    
    private static class PostSequenceConfig {
        String title;
        String subtitle;
        boolean musicEnabled;
        String musicSound;
        float musicVolume;
        float musicPitch;
        int musicLoopTicks;
    }

    public FreezeManager(@NotNull VoiidCountdownTimer plugin) {
        this.plugin = plugin;
        this.freezeConfigManager = VoiidCountdownTimer.getConfigsManager().getFreezeConfigManager();
        this.mainConfigManager = VoiidCountdownTimer.getConfigsManager().getMainConfigManager();
        this.messagesManager = VoiidCountdownTimer.getMessagesManager();
        this.freezeStateFile = new File(plugin.getDataFolder(), "freeze-state.yml");
        this.successConfig = new StateConfig();
        this.failConfig = new StateConfig();
        this.reload();
    }

    public void reload() {
        FileConfiguration config = this.freezeConfigManager.getConfig();
        
        // Load core settings
        this.killMobsOnFreeze = config.getBoolean("Freeze.core.mobs.kill_on_freeze", true);
        this.preventMobSpawn = config.getBoolean("Freeze.core.mobs.prevent_spawn", true);
        this.muteChat = config.getBoolean("Freeze.core.notifications.mute_chat", false);
        this.silentNotifications = config.getBoolean("Freeze.core.notifications.silent", false);
        this.notifyCooldownMs = Math.max(0L, config.getLong("Freeze.core.notifications.cooldown_ms", 1500L));
        this.lockMidnight = config.getBoolean("Freeze.core.time.lock_midnight", true);
        
        // Load success state
        this.successConfig = loadStateConfig(config, "success");
        
        // Load fail state
        this.failConfig = loadStateConfig(config, "fail");
        
        // Restore state if frozen
        if (this.frozen) {
            loadAndApplyPersistentState();
        }
    }
    
    private StateConfig loadStateConfig(FileConfiguration config, String stateName) {
        StateConfig state = new StateConfig();
        String prefix = "Freeze." + stateName + ".";
        
        // Initial sound
        state.initialSoundEnabled = config.getBoolean(prefix + "initial_sound.enabled", false);
        state.initialSoundSound = config.getString(prefix + "initial_sound.sound", null);
        state.initialSoundVolume = (float) config.getDouble(prefix + "initial_sound.volume", 1.0);
        state.initialSoundPitch = (float) config.getDouble(prefix + "initial_sound.pitch", 1.0);
        double soundLengthSeconds = config.getDouble(prefix + "initial_sound.sound_length", 0.0);
        state.initialSoundLengthTicks = Math.max(0, secondsToTicks(soundLengthSeconds));
        
        // Music
        state.musicEnabled = config.getBoolean(prefix + "music.enabled", false);
        state.musicSound = config.getString(prefix + "music.sound", null);
        state.musicVolume = (float) config.getDouble(prefix + "music.volume", 1.0);
        state.musicPitch = (float) config.getDouble(prefix + "music.pitch", 1.0);
        double loopSeconds = config.getDouble(prefix + "music.loop_seconds", 20.0);
        state.musicLoopTicks = Math.max(20, secondsToTicks(loopSeconds));
        
        // Sequence
        state.sequence.clear();
        List<Map<?, ?>> sequenceList = config.getMapList(prefix + "sequence");
        for (Map<?, ?> raw : sequenceList) {
            Object titleObj = raw.get("title");
            Object subtitleObj = raw.get("subtitle");
            String titleStr = titleObj != null ? String.valueOf(titleObj) : "";
            String subtitleStr = subtitleObj != null ? String.valueOf(subtitleObj) : "";
            
            // Don't colorize JSON strings
            String title = looksLikeJson(titleStr) ? titleStr : colorize(titleStr);
            String subtitle = looksLikeJson(subtitleStr) ? subtitleStr : colorize(subtitleStr);
            
            int fadeIn = secondsToTicks(parseDouble(raw.get("fade_in_seconds"), 0.0));
            int stay = secondsToTicks(parseDouble(raw.get("stay_seconds"), 1.0));
            int fadeOut = secondsToTicks(parseDouble(raw.get("fade_out_seconds"), 0.0));
            
            List<String> commands = new ArrayList<>();
            Object cmdObj = raw.get("commands");
            if (cmdObj instanceof List) {
                for (Object cmd : (List<?>) cmdObj) {
                    if (cmd != null) {
                        commands.add(String.valueOf(cmd));
                    }
                }
            } else if (cmdObj instanceof String) {
                commands.add((String) cmdObj);
            }
            
            state.sequence.add(new SequenceStep(title, subtitle, fadeIn, stay, fadeOut, commands));
        }
        
        // Post-sequence
        String postPrefix = prefix + "post_sequence.";
        state.postSequence = new PostSequenceConfig();
        Object postTitleObj = config.get(postPrefix + "title");
        Object postSubtitleObj = config.get(postPrefix + "subtitle");
        String postTitleStr = postTitleObj != null ? String.valueOf(postTitleObj) : "";
        String postSubtitleStr = postSubtitleObj != null ? String.valueOf(postSubtitleObj) : "";
        state.postSequence.title = looksLikeJson(postTitleStr) ? postTitleStr : colorize(postTitleStr);
        state.postSequence.subtitle = looksLikeJson(postSubtitleStr) ? postSubtitleStr : colorize(postSubtitleStr);
        
        state.postSequence.musicEnabled = config.getBoolean(postPrefix + "music.enabled", false);
        state.postSequence.musicSound = config.getString(postPrefix + "music.sound", null);
        state.postSequence.musicVolume = (float) config.getDouble(postPrefix + "music.volume", 1.0);
        state.postSequence.musicPitch = (float) config.getDouble(postPrefix + "music.pitch", 1.0);
        double postLoopSeconds = config.getDouble(postPrefix + "music.loop_seconds", 20.0);
        state.postSequence.musicLoopTicks = Math.max(20, secondsToTicks(postLoopSeconds));
        
        return state;
    }

    public boolean isFrozen() {
        return this.frozen;
    }

    public boolean isMobsFrozen() {
        return this.mobsFrozen;
    }

    public void freeze(boolean freezeMobs) {
        if (this.frozen) {
            this.mobsFrozen = freezeMobs;
            if (freezeMobs) {
                freezeAllMobs();
            } else {
                unfreezeAllMobs();
            }
            refreshPlayers();
            saveFreezeState();
            return;
        }
        
        this.frozen = true;
        this.mobsFrozen = freezeMobs;
        // Don't clear currentState if already set (freezeWithVariant will have set it)
        this.inPostSequence = false;
        
        // Apply freeze effects
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                applyPlayerFreeze(player);
            }
        });
        
        if (freezeMobs) {
            freezeAllMobs();
        }
        if (this.killMobsOnFreeze) {
            killAllMobs();
        }
        if (this.lockMidnight) {
            lockWorldsToMidnight();
        }
        
        saveFreezeState();
    }
    
    public void freezeWithVariant(boolean freezeMobs, @NotNull FreezeVariant variant) {
        // Determine state first
        String targetState;
        if (variant == FreezeVariant.SUCCESS) {
            targetState = "success";
        } else if (variant == FreezeVariant.FAIL) {
            targetState = "fail";
        } else {
            targetState = null;
        }
        
        if (!this.frozen) {
            // Set state before calling freeze to avoid it being cleared
            this.currentState = targetState;
            freeze(freezeMobs);
        } else {
            this.currentState = targetState;
            this.mobsFrozen = freezeMobs;
            if (freezeMobs) {
                freezeAllMobs();
            } else {
                unfreezeAllMobs();
            }
        }
        
        // Start the state sequence
        startStateSequence();
    }

    public void unfreeze() {
        if (!this.frozen) {
            return;
        }
        
        this.frozen = false;
        this.mobsFrozen = false;
        this.currentState = null;
        this.inPostSequence = false;
        
        // Cancel all tasks
        cancelSequenceTasks();
        stopMusicTask();
        stopTimeTask();
        
        // Release players
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                releasePlayer(player);
            }
            clearAllTitles(p -> true);
        });
        
        unfreezeAllMobs();
        if (this.lockMidnight) {
            restoreWorldTime();
        }
        
        saveFreezeState();
    }

    private void startStateSequence() {
        if (this.currentState == null) {
            return;
        }
        
        StateConfig config = this.currentState.equals("success") ? this.successConfig : this.failConfig;
        
        // Cancel any existing sequence
        cancelSequenceTasks();
        
        // Play initial sound
        if (config.initialSoundEnabled && config.initialSoundSound != null) {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playSound(player, config.initialSoundSound, config.initialSoundVolume, config.initialSoundPitch);
                }
            });
            
            // Start music after initial sound length
            if (config.musicEnabled && config.musicSound != null) {
                long delayTicks = Math.max(0L, (long) config.initialSoundLengthTicks);
                BukkitTask musicStartTask = Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    startMusicLoop(config);
                }, delayTicks);
                this.sequenceTasks.add(musicStartTask);
            }
        } else {
            // No initial sound - start music immediately
            if (config.musicEnabled && config.musicSound != null) {
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    startMusicLoop(config);
                });
            }
        }
        
        // Execute sequence steps sequentially
        long currentTime = 0L;
        String persistentTitle = null; // Track title across steps
        boolean firstTitleSet = false; // Track if we've set the initial title
        
        // Calculate total duration for each step to determine stay times
        List<Long> stepDurations = new ArrayList<>();
        for (SequenceStep step : config.sequence) {
            stepDurations.add((long)(step.fadeInTicks + step.stayTicks + step.fadeOutTicks));
        }
        
        for (int i = 0; i < config.sequence.size(); i++) {
            SequenceStep step = config.sequence.get(i);
            long stepStartTime = currentTime;
            long stepDuration = stepDurations.get(i);
            
            // Track if title changes in this step
            boolean titleChanges = step.title != null && !step.title.trim().isEmpty();
            
            // Update persistent title if current step has a non-empty title
            if (titleChanges) {
                persistentTitle = step.title;
            }
            
            // Use persistent title for display
            final String displayTitle = persistentTitle != null ? persistentTitle : step.title;
            final boolean isFirstTitle = !firstTitleSet && titleChanges;
            if (titleChanges) {
                firstTitleSet = true;
            }
            
            // Execute commands at step start
            if (!step.commands.isEmpty()) {
                BukkitTask cmdTask = Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    executeCommands(step.commands);
                }, stepStartTime);
                this.sequenceTasks.add(cmdTask);
            }
            
            // For the first title with actual content, send full title+subtitle with timing
            // For subsequent steps, only update subtitle via command to avoid redraw
            if (isFirstTitle) {
                // First title - send with full timing
                final int extendedStay = Integer.MAX_VALUE / 20; // Very long stay
                BukkitTask titleTask = Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        // Set timing once with a very long duration
                        sendTitleTiming(player, step.fadeInTicks, extendedStay, 0);
                        sendTitleSmart(player, displayTitle, step.subtitle, step.fadeInTicks, extendedStay, 0);
                    }
                }, stepStartTime);
                this.sequenceTasks.add(titleTask);
            } else {
                // Subsequent steps - only update subtitle without touching title
                BukkitTask subtitleTask = Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        sendSubtitleCommand(player, step.subtitle);
                    }
                }, stepStartTime);
                this.sequenceTasks.add(subtitleTask);
            }
            
            // Move to next step after this step's duration completes
            currentTime += stepDuration;
        }
        
        // After sequence completes, show post-sequence state
        if (currentTime > 0) {
            PostSequenceConfig postSeq = config.postSequence;
            BukkitTask postSeqTask = Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                this.inPostSequence = true;
                
                stopMusicTask();
                if (postSeq.musicEnabled && postSeq.musicSound != null) {
                    startPostSequenceMusic(postSeq);
                }
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (postSeq.title != null && !postSeq.title.trim().isEmpty()) {
                        int indefiniteStay = Integer.MAX_VALUE / 20;
                        sendTitleTiming(player, 0, indefiniteStay, 0);
                        sendTitleSmart(player, postSeq.title, postSeq.subtitle, 0, indefiniteStay, 0);
                    } else {
                        sendSubtitleCommand(player, postSeq.subtitle);
                    }
                }
                saveFreezeState();
            }, currentTime);
            this.sequenceTasks.add(postSeqTask);
        } else {
            PostSequenceConfig postSeq = config.postSequence;
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                this.inPostSequence = true;
                
                if (postSeq.musicEnabled && postSeq.musicSound != null) {
                    startPostSequenceMusic(postSeq);
                }
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (postSeq.title != null && !postSeq.title.trim().isEmpty()) {
                        int indefiniteStay = Integer.MAX_VALUE / 20;
                        sendTitleTiming(player, 0, indefiniteStay, 0);
                        sendTitleSmart(player, postSeq.title, postSeq.subtitle, 0, indefiniteStay, 0);
                    } else {
                        sendSubtitleCommand(player, postSeq.subtitle);
                    }
                }
                saveFreezeState();
            });
        }
    }
    
    private void startMusicLoop(StateConfig config) {
        stopMusicTask();
        if (!config.musicEnabled || config.musicSound == null) {
            return;
        }
        
        final int period = Math.max(10, config.musicLoopTicks);
        this.musicTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (!this.frozen || (this.currentState == null)) {
                stopMusicTask();
            return;
        }
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    if (config.musicSound != null) {
                        player.stopSound(config.musicSound);
                    }
                } catch (Throwable ignored) {}
                
                playSound(player, config.musicSound, config.musicVolume, config.musicPitch);
            }
        }, 0L, (long) period);
    }
    
    private void stopMusicTask() {
        if (this.musicTask != null) {
            try {
                this.musicTask.cancel();
            } catch (Throwable ignored) {}
            this.musicTask = null;
        }
    }
    
    private void startPostSequenceMusic(PostSequenceConfig postSeq) {
        stopMusicTask();
        if (!postSeq.musicEnabled || postSeq.musicSound == null) {
            return;
        }
        
        final int period = Math.max(10, postSeq.musicLoopTicks);
        this.musicTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (!this.frozen || !this.inPostSequence) {
                stopMusicTask();
                return;
            }
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    if (postSeq.musicSound != null) {
                        player.stopSound(postSeq.musicSound);
                    }
                } catch (Throwable ignored) {}
                
                playSound(player, postSeq.musicSound, postSeq.musicVolume, postSeq.musicPitch);
            }
        }, 0L, (long) period);
    }
    
    private void cancelSequenceTasks() {
        for (BukkitTask task : this.sequenceTasks) {
            try {
                task.cancel();
            } catch (Throwable ignored) {}
        }
        this.sequenceTasks.clear();
    }
    
    private void executeCommands(List<String> commands) {
        org.bukkit.command.CommandSender console = Bukkit.getConsoleSender();
        for (String cmd : commands) {
            if (cmd != null && !cmd.trim().isEmpty()) {
                try {
                    Bukkit.dispatchCommand(console, cmd.trim());
                } catch (Throwable ignored) {}
            }
        }
    }
    
    public void applyPlayerFreeze(@NotNull Player player) {
        player.closeInventory();
        player.setVelocity(player.getVelocity().zero());
    }
    
    public void releasePlayer(@NotNull Player player) {
        this.lastNotification.remove(player.getUniqueId());
    }
    
    public void handlePlayerJoin(@NotNull Player player) {
        if (this.frozen) {
            applyPlayerFreeze(player);
            
            if (this.currentState != null) {
                StateConfig config = this.currentState.equals("success") ? this.successConfig : this.failConfig;
                PostSequenceConfig postSeq = config.postSequence;
                
                String displayTitle;
                String displaySubtitle;
                
                if (this.inPostSequence) {
                    if (postSeq.title != null && !postSeq.title.trim().isEmpty()) {
                        displayTitle = postSeq.title;
                    } else {
                        String persistentTitle = null;
                        for (SequenceStep step : config.sequence) {
                            if (step.title != null && !step.title.trim().isEmpty()) {
                                persistentTitle = step.title;
                            }
                        }
                        displayTitle = persistentTitle;
                    }
                    displaySubtitle = postSeq.subtitle != null ? postSeq.subtitle : "";
                } else {
                    String persistentTitle = null;
                    for (SequenceStep step : config.sequence) {
                        if (step.title != null && !step.title.trim().isEmpty()) {
                            persistentTitle = step.title;
                        }
                    }
                    displayTitle = persistentTitle;
                    displaySubtitle = "";
                }
                
                final String finalTitle = displayTitle;
                final String finalSubtitle = displaySubtitle;
                
                if (finalTitle != null && !finalTitle.isEmpty()) {
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        int indefiniteStay = Integer.MAX_VALUE / 20;
                        sendTitleTiming(player, 0, indefiniteStay, 0);
                        sendTitleSmart(player, finalTitle, finalSubtitle, 0, indefiniteStay, 0);
                    });
                }
            }
        }
    }
    
    public void handlePlayerQuit(@NotNull Player player) {
        this.lastNotification.remove(player.getUniqueId());
    }
    
    public void handleMobSpawn(@NotNull Mob mob) {
        if (this.frozen && this.mobsFrozen) {
            if (!this.mobAiSnapshot.containsKey(mob.getUniqueId())) {
                this.mobAiSnapshot.put(mob.getUniqueId(), mob.hasAI());
            }
            mob.setAI(false);
        }
    }
    
    public boolean shouldCancelPlayer(@NotNull Player player) {
        if (!this.frozen) {
            return false;
        }
        // Check bypass permission if honor_bypass_permission is enabled
        // For now, just check if frozen
        return true;
    }
    
    public void notifyBlocked(@NotNull Player player, String configPath) {
        if (this.muteChat && configPath.contains("Chat")) {
            return; // Don't notify if chat is muted
        }
        
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastNotify = this.lastNotification.get(uuid);
        
        if (lastNotify != null && (now - lastNotify) < this.notifyCooldownMs) {
            return; // Cooldown active
        }
        
        this.lastNotification.put(uuid, now);
        
        if (this.messagesManager != null && configPath != null) {
            this.messagesManager.sendConfigMessage(player, configPath, true, null);
        }
    }
    
    public boolean shouldPreventMobSpawn() {
        return this.preventMobSpawn;
    }
    
    public boolean isSilentNotifications() {
        return this.silentNotifications;
    }
    
    public void handleTeleport(@NotNull Player player, org.bukkit.event.player.PlayerTeleportEvent event) {
        if (this.frozen && shouldCancelPlayer(player)) {
            // Only cancel teleports that aren't caused by plugins (like world changes)
            if (event.getCause() != org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN &&
                event.getCause() != org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.UNKNOWN) {
                event.setCancelled(true);
                notifyBlocked(player, "Messages.freezeBlockedAction");
            }
        }
    }
    
    private void refreshPlayers() {
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                applyPlayerFreeze(player);
            }
        });
    }

    private void freezeAllMobs() {
        for (World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
            if (entity instanceof Mob) {
                    Mob mob = (Mob) entity;
                    if (!this.mobAiSnapshot.containsKey(entity.getUniqueId())) {
                        this.mobAiSnapshot.put(entity.getUniqueId(), mob.hasAI());
                    }
                mob.setAI(false);
            }
    }
        }
    }

    private void unfreezeAllMobs() {
        for (Map.Entry<UUID, Boolean> entry : this.mobAiSnapshot.entrySet()) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity instanceof Mob) {
                ((Mob) entity).setAI(entry.getValue());
            }
        }
            this.mobAiSnapshot.clear();
    }

    private void killAllMobs() {
        for (World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity instanceof Mob && !(entity instanceof Player)) {
                    entity.remove();
                }
            }
        }
    }

    private void lockWorldsToMidnight() {
        for (World world : Bukkit.getWorlds()) {
                if (!this.prevDaylight.containsKey(world)) {
                Boolean current = world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE);
                    this.prevDaylight.put(world, current != null ? current : Boolean.TRUE);
            }
            if (!this.prevWorldTime.containsKey(world)) {
                this.prevWorldTime.put(world, world.getTime());
            }
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setTime(18000L); // Midnight
        }
        
        if (this.timeTask == null) {
            this.timeTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
                if (!this.frozen || !this.lockMidnight) {
                    stopTimeTask();
                    return;
                }
                for (World world : Bukkit.getWorlds()) {
            world.setTime(18000L);
        }
            }, 0L, 100L); // Every 5 seconds
        }
    }

    private void restoreWorldTime() {
        for (Map.Entry<World, Boolean> entry : this.prevDaylight.entrySet()) {
            entry.getKey().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, entry.getValue());
        }
        for (Map.Entry<World, Long> entry : this.prevWorldTime.entrySet()) {
            entry.getKey().setTime(entry.getValue());
        }
        this.prevDaylight.clear();
        this.prevWorldTime.clear();
    }

    private void stopTimeTask() {
        if (this.timeTask != null) {
            try {
                this.timeTask.cancel();
            } catch (Throwable ignored) {}
            this.timeTask = null;
        }
    }

    private void playSound(Player player, String soundName, float volume, float pitch) {
        if (soundName == null) return;
        try {
            player.playSound(player.getLocation(), soundName, volume, pitch);
        } catch (Throwable t) {
            try {
                Sound snd = SoundResolver.find(soundName);
                if (snd != null) {
                    player.playSound(player.getLocation(), snd, volume, pitch);
                }
                    } catch (Throwable ignored) {}
        }
    }
    
    private void sendTitleSmart(@NotNull Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        boolean titleIsJson = looksLikeJson(title);
        boolean subtitleIsJson = looksLikeJson(subtitle);
        
        if (titleIsJson || subtitleIsJson) {
            if (trySendNativeJsonTitle(player, title, subtitle, fadeIn, stay, fadeOut)) {
                return;
            }
        }

        // Try Adventure API
        try {
            Class<?> componentClass = tryLoad("net.kyori.adventure.text.Component");
            if (componentClass != null) {
                Object titleComp = toComponentForced(player, title);
                Object subtitleComp = toComponentForced(player, subtitle);
                if (tryShowAdventureTitle(player, titleComp, subtitleComp, fadeIn, stay, fadeOut)) {
                    return;
                }
            }
        } catch (Throwable ignored) {}

        // Legacy fallback
        try {
            player.sendTitle(title != null ? title : "", subtitle != null ? subtitle : "", 
                Math.max(0, fadeIn), Math.max(1, stay), Math.max(0, fadeOut));
        } catch (Throwable ignored) {}
    }
    
    private void sendTitleTiming(@NotNull Player player, int fadeIn, int stay, int fadeOut) {
        try {
            String name = player.getName();
            org.bukkit.command.CommandSender console = Bukkit.getConsoleSender();
            Bukkit.dispatchCommand(console, String.format("title %s times %d %d %d", 
                name, Math.max(0, fadeIn), Math.max(1, stay), Math.max(0, fadeOut)));
        } catch (Throwable ignored) {}
    }
    
    private void sendSubtitleCommand(@NotNull Player player, String subtitle) {
        try {
            String name = player.getName();
            org.bukkit.command.CommandSender console = Bukkit.getConsoleSender();
            if (subtitle != null) {
                String jsonSubtitle = looksLikeJson(subtitle) ? subtitle.trim() : toJsonStringLiteral(subtitle);
                Bukkit.dispatchCommand(console, String.format("title %s subtitle %s", name, jsonSubtitle));
            }
        } catch (Throwable ignored) {}
    }

    private boolean trySendNativeJsonTitle(@NotNull Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            String name = player.getName();
            org.bukkit.command.CommandSender console = Bukkit.getConsoleSender();
            Bukkit.dispatchCommand(console, String.format("title %s times %d %d %d", name, Math.max(0, fadeIn), Math.max(1, stay), Math.max(0, fadeOut)));
            if (title != null) {
                String jsonTitle = looksLikeJson(title) ? title.trim() : toJsonStringLiteral(title);
                Bukkit.dispatchCommand(console, String.format("title %s title %s", name, jsonTitle));
            }
            if (subtitle != null) {
                String jsonSubtitle = looksLikeJson(subtitle) ? subtitle.trim() : toJsonStringLiteral(subtitle);
                Bukkit.dispatchCommand(console, String.format("title %s subtitle %s", name, jsonSubtitle));
            }
            return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private static String toJsonStringLiteral(String text) {
        if (text == null) return "\"\"";
        String s = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "\"" + s + "\"";
    }

    private boolean tryShowAdventureTitle(@NotNull Player player, Object titleComponent, Object subtitleComponent, int fadeIn, int stay, int fadeOut) {
        try {
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Class<?> titleClass = Class.forName("net.kyori.adventure.title.Title");
            Class<?> timesClass = Class.forName("net.kyori.adventure.title.Title$Times");

            java.time.Duration fi = java.time.Duration.ofMillis(Math.max(0, fadeIn) * 50L);
            java.time.Duration st = java.time.Duration.ofMillis(Math.max(1, stay) * 50L);
            java.time.Duration fo = java.time.Duration.ofMillis(Math.max(0, fadeOut) * 50L);
            java.lang.reflect.Method timesFactory = timesClass.getMethod("times", java.time.Duration.class, java.time.Duration.class, java.time.Duration.class);
            Object times = timesFactory.invoke(null, fi, st, fo);

            java.lang.reflect.Method titleFactory = titleClass.getMethod("title", componentClass, componentClass, timesClass);
            Object advTitle = titleFactory.invoke(null, titleComponent, subtitleComponent, times);

            for (java.lang.reflect.Method m : player.getClass().getMethods()) {
                if (!m.getName().equals("showTitle")) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 1 && params[0].getName().equals(titleClass.getName())) {
                    m.invoke(player, advTitle);
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private Object toComponentForced(@NotNull Player player, String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        if (looksLikeJson(text)) {
            Object jsonComp = tryParseJsonToComponent(text);
            if (jsonComp != null) return jsonComp;
        }
        
        try {
            Object formatted = this.mainConfigManager.getFormatter().format(this.plugin, 
                Bukkit.getConsoleSender(), text);
            if (formatted != null) {
                Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
                if (componentClass.isInstance(formatted)) {
                    return formatted;
                }
            }
        } catch (Throwable ignored) {}
        
        return null;
    }

    private Object tryParseJsonToComponent(String json) {
        try {
            Class<?> gsonClass = Class.forName("com.google.gson.Gson");
            Object gson = gsonClass.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method fromJson = gsonClass.getMethod("fromJson", String.class, Class.class);
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            return fromJson.invoke(gson, json, componentClass);
        } catch (Throwable ignored) {}
        return null;
    }

    private void clearAllTitles(java.util.function.Predicate<Player> filter) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (filter != null && !filter.test(p)) continue;
            try {
                p.resetTitle();
            } catch (Throwable ignored) {
                try {
                    p.sendTitle("", "", 0, 1, 0);
                } catch (Throwable ignored2) {}
            }
        }
    }
    
    private boolean looksLikeJson(String s) {
        if (s == null) return false;
        String t = s.trim();
        return t.startsWith("{") || t.startsWith("[");
    }
    
    private String colorize(String input) {
        if (input == null) {
            return "";
        }
        String unescaped = unescapeUnicodeEscapes(input);
        return ChatColor.translateAlternateColorCodes('&', unescaped);
    }
    
    private String unescapeUnicodeEscapes(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == 'u' || n == 'U') {
                    int start = i + 2;
                    int end = start + 4;
                    if (end <= s.length()) {
                        String hex = s.substring(start, end);
                        try {
                            int code = Integer.parseInt(hex, 16);
                            out.append((char) code);
                            i = end - 1;
                            continue;
                        } catch (NumberFormatException ignored) {}
                    }
                } else {
                    if (n == 'n') { out.append('\n'); ++i; continue; }
                    if (n == 'r') { out.append('\r'); ++i; continue; }
                    if (n == 't') { out.append('\t'); ++i; continue; }
                    if (n == 'b') { out.append('\b'); ++i; continue; }
                    if (n == 'f') { out.append('\f'); ++i; continue; }
                }
            }
            out.append(c);
        }
        return out.toString();
    }
    
    private int secondsToTicks(double seconds) {
        return (int) Math.round(Math.max(0.0, seconds) * 20.0);
    }
    
    private double parseDouble(Object value, double defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }
    
    private Class<?> tryLoad(String className) {
        try {
            return Class.forName(className);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void saveFreezeState() {
        try {
            FileConfiguration state = new YamlConfiguration();
            state.set("frozen", this.frozen);
            state.set("mobs_frozen", this.mobsFrozen);
            state.set("current_state", this.currentState);
            state.set("in_post_sequence", this.inPostSequence);
            state.save(this.freezeStateFile);
        } catch (IOException ignored) {}
    }
    
    public void loadAndApplyPersistentState() {
        if (!this.freezeStateFile.exists()) {
            return;
        }
        
        try {
            FileConfiguration state = YamlConfiguration.loadConfiguration(this.freezeStateFile);
            this.frozen = state.getBoolean("frozen", false);
            this.mobsFrozen = state.getBoolean("mobs_frozen", false);
            this.currentState = state.getString("current_state", null);
            this.inPostSequence = state.getBoolean("in_post_sequence", false);
            
            if (this.frozen) {
                // Apply freeze effects
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        applyPlayerFreeze(player);
                    }
                });
                
                if (this.mobsFrozen) {
                    freezeAllMobs();
                }
                if (this.killMobsOnFreeze) {
                    killAllMobs();
        }
        if (this.lockMidnight) {
                    lockWorldsToMidnight();
                }
                
                if (this.inPostSequence && this.currentState != null) {
                    StateConfig config = this.currentState.equals("success") ? this.successConfig : this.failConfig;
                    PostSequenceConfig postSeq = config.postSequence;
                    
                    String displayTitle;
                    if (postSeq.title != null && !postSeq.title.trim().isEmpty()) {
                        displayTitle = postSeq.title;
                    } else {
                        String persistentTitle = null;
                        for (SequenceStep step : config.sequence) {
                            if (step.title != null && !step.title.trim().isEmpty()) {
                                persistentTitle = step.title;
                            }
                        }
                        displayTitle = persistentTitle;
                    }
                    
                    final String finalTitle = displayTitle;
                    
                    if (postSeq.musicEnabled && postSeq.musicSound != null) {
                        startPostSequenceMusic(postSeq);
                    }
                    
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            int indefiniteStay = Integer.MAX_VALUE / 20;
                            if (finalTitle != null && !finalTitle.isEmpty()) {
                                sendTitleTiming(player, 0, indefiniteStay, 0);
                                sendTitleSmart(player, finalTitle, postSeq.subtitle != null ? postSeq.subtitle : "", 0, indefiniteStay, 0);
                            } else {
                                plugin.getLogger().warning("Failed to restore freeze title - no persistent title found in sequence");
                            }
                        }
                    });
                } else if (this.currentState != null) {
                    // Restart sequence
                    startStateSequence();
                }
            }
        } catch (Exception ignored) {}
    }
    
    public void shutdownForDisable() {
        saveFreezeState();
        cancelSequenceTasks();
        stopMusicTask();
        stopTimeTask();
        if (this.lockMidnight) {
            restoreWorldTime();
        }
        unfreezeAllMobs();
        for (Player player : Bukkit.getOnlinePlayers()) {
            releasePlayer(player);
            clearAllTitles(p -> p.equals(player));
        }
    }
    
    // Legacy API compatibility
    public void setTitleVariant(@NotNull FreezeVariant variant) {
        if (variant == FreezeVariant.SUCCESS) {
            this.currentState = "success";
        } else if (variant == FreezeVariant.FAIL) {
            this.currentState = "fail";
        } else {
            this.currentState = null;
        }
    }
    
    public FreezeVariant getTitleVariant() {
        if ("success".equals(this.currentState)) {
            return FreezeVariant.SUCCESS;
        } else if ("fail".equals(this.currentState)) {
            return FreezeVariant.FAIL;
        }
        return FreezeVariant.DEFAULT;
    }
}
