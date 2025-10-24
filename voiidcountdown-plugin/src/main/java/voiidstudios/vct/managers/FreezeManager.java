/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.GameRule
 *  org.bukkit.Location
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Mob
 *  org.bukkit.entity.Player
 *  org.bukkit.event.player.PlayerTeleportEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitTask
 *  org.jetbrains.annotations.NotNull
 *  voiidstudios.vct.VoiidCountdownTimer
 *  voiidstudios.vct.configs.MainConfigManager
 *  voiidstudios.vct.managers.MessagesManager
 *  voiidstudios.vct.managers.SpawnBookManager
 *  voiidstudios.vct.utils.SoundResolver
 */
package voiidstudios.vct.managers;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.configs.MainConfigManager;
// Note: avoid import warnings in some environments by qualifying types where used
import voiidstudios.vct.utils.SoundResolver;

public class FreezeManager {
    private final VoiidCountdownTimer plugin;
    private final MainConfigManager configManager;
    private final voiidstudios.vct.managers.MessagesManager messagesManager;
    private boolean frozen;
    private boolean mobsFrozen;
    private final Map<UUID, Boolean> mobAiSnapshot = new HashMap<UUID, Boolean>();
    private final Map<UUID, PotionEffect> priorBlindness = new HashMap<UUID, PotionEffect>();
    private final Map<UUID, Long> lastNotification = new HashMap<UUID, Long>();
    private final Set<UUID> appliedBlindness = new HashSet<UUID>();
    private String titleText;
    private String subtitleText;
    private int titleFadeIn;
    private int titleStay;
    private int titleFadeOut;
    private boolean blindnessEnabled;
    private int blindnessAmplifier;
    private boolean blindnessAmbient;
    private boolean blindnessParticles;
    private long notifyCooldownMs;
    private boolean silentNotifications;
    private boolean defaultFreezeMobs;
    private boolean preventMobSpawn;
    private boolean killMobsOnFreeze;
    private boolean lockMidnight;
    private int titleKeepaliveTicks;
    private BukkitTask titleTask;
    private BukkitTask timeTask;
    private BukkitTask musicTask;
    private final Map<UUID, Long> lastMusicPlay = new HashMap<UUID, Long>();
    private File freezeStateFile;
    private final Map<World, Boolean> prevDaylight = new HashMap<World, Boolean>();
    private final Map<World, Long> prevWorldTime = new HashMap<World, Long>();
    private boolean musicEnabled;
    private String musicSound;
    private float musicVolume;
    private float musicPitch;
    private int musicLoopTicks;

    public FreezeManager(@NotNull VoiidCountdownTimer plugin) {
        this.plugin = plugin;
        this.configManager = VoiidCountdownTimer.getConfigsManager().getMainConfigManager();
        this.messagesManager = VoiidCountdownTimer.getMessagesManager();
        this.freezeStateFile = new File(plugin.getDataFolder(), "freeze-state.yml");
        this.reload();
    }

    public void reload() {
        FileConfiguration config = this.configManager.getConfig();
        this.titleText = this.colorize(config.getString("Freeze.title.text", "&cServer Frozen"));
        this.subtitleText = this.colorize(config.getString("Freeze.title.subtitle", "&7Please wait"));
        this.titleFadeIn = Math.max(0, config.getInt("Freeze.title.fade_in", 10));
        this.titleStay = Math.max(1, config.getInt("Freeze.title.stay", 60));
        this.titleFadeOut = Math.max(0, config.getInt("Freeze.title.fade_out", 20));
        this.blindnessEnabled = config.getBoolean("Freeze.blindness.enabled", true);
        this.blindnessAmplifier = Math.max(0, config.getInt("Freeze.blindness.amplifier", 0));
        this.blindnessAmbient = config.getBoolean("Freeze.blindness.ambient", false);
        this.blindnessParticles = config.getBoolean("Freeze.blindness.particles", false);
        this.notifyCooldownMs = Math.max(0L, config.getLong("Freeze.notifications.cooldown_ms", 1500L));
        this.silentNotifications = config.getBoolean("Freeze.notifications.silent", false);
        this.defaultFreezeMobs = config.getBoolean("Freeze.default_freeze_mobs", false);
        this.preventMobSpawn = config.getBoolean("Freeze.mobs.prevent_spawn", true);
        this.killMobsOnFreeze = config.getBoolean("Freeze.mobs.kill_on_freeze", true);
        this.lockMidnight = config.getBoolean("Freeze.time.lock_midnight", true);
        int timeKeepalive = Math.max(1, config.getInt("Freeze.time.keepalive_ticks", 100));
        this.musicEnabled = config.getBoolean("Freeze.music.enabled", false);
        this.musicSound = config.getString("Freeze.music.sound", "minecraft:music.menu");
        this.musicVolume = (float)config.getDouble("Freeze.music.volume", 1.0);
        this.musicPitch = (float)config.getDouble("Freeze.music.pitch", 1.0);
        int legacyTicks = config.getInt("Freeze.music.loop_ticks", -1);
        double loopSeconds = config.getDouble("Freeze.music.loop_seconds", legacyTicks > 0 ? (double)legacyTicks / 20.0 : 20.0);
        this.musicLoopTicks = Math.max(20, (int)Math.round(loopSeconds * 20.0));
        this.titleKeepaliveTicks = Math.max(1, config.getInt("Freeze.title.keepalive_ticks", 40));
        if (this.frozen) {
            Bukkit.getScheduler().runTask((Plugin)this.plugin, this::refreshPlayers);
            this.restartTitleTask();
            if (this.lockMidnight) {
                this.restartTimeTask(timeKeepalive);
            }
            if (this.musicEnabled) {
                this.restartMusicTask();
            }
        }
    }

    private String colorize(String input) {
        if (input == null) {
            return "";
        }
        String unescaped = this.unescapeUnicodeEscapes(input);
        return ChatColor.translateAlternateColorCodes((char)'&', (String)unescaped);
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
                            out.append((char)code);
                            i = end - 1;
                            continue;
                        }
                        catch (NumberFormatException numberFormatException) {
                        }
                    }
                } else {
                    if (n == 'n') {
                        out.append('\n');
                        ++i;
                        continue;
                    }
                    if (n == 'r') {
                        out.append('\r');
                        ++i;
                        continue;
                    }
                    if (n == 't') {
                        out.append('\t');
                        ++i;
                        continue;
                    }
                    if (n == 'b') {
                        out.append('\b');
                        ++i;
                        continue;
                    }
                    if (n == 'f') {
                        out.append('\f');
                        ++i;
                        continue;
                    }
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    public boolean isFrozen() {
        return this.frozen;
    }

    public boolean isMobsFrozen() {
        return this.mobsFrozen;
    }

    public boolean getDefaultFreezeMobs() {
        return this.defaultFreezeMobs;
    }

    public void freeze(boolean freezeMobs) {
        if (this.frozen) {
            this.mobsFrozen = freezeMobs;
            if (freezeMobs) {
                this.freezeAllMobs();
            } else {
                this.unfreezeAllMobs();
            }
            this.refreshPlayers();
            this.saveFreezeState();
            return;
        }
        this.frozen = true;
        this.mobsFrozen = freezeMobs;
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                this.applyPlayerFreeze(player);
            }
        });
        if (freezeMobs) {
            this.freezeAllMobs();
        }
        this.restartTitleTask();
        if (this.killMobsOnFreeze) {
            this.killAllMobs();
        }
        if (this.lockMidnight) {
            this.lockWorldsToMidnight();
        }
        if (this.musicEnabled) {
            this.restartMusicTask();
        }
        this.saveFreezeState();
    }

    public void unfreeze() {
        if (!this.frozen) {
            return;
        }
        this.frozen = false;
        this.mobsFrozen = false;
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                this.releasePlayer(player);
            }
        });
        this.unfreezeAllMobs();
        this.stopTitleTask();
        if (this.lockMidnight) {
            this.restoreWorldTime();
            this.stopTimeTask();
        }
        this.stopMusicTask();
        this.saveFreezeState();
    }

    public void applyPlayerFreeze(@NotNull Player player) {
        if (!this.frozen || player.hasPermission("voiidcountdowntimer.freeze.bypass")) {
            return;
        }
        player.closeInventory();
        player.setVelocity(player.getVelocity().zero());
        this.sendFreezeTitle(player);
        this.applyBlindness(player);
        this.purgeProphecyBooks(player);
        if (this.musicEnabled) {
            this.playMusicFor(player);
        }
    }

    public void releasePlayer(@NotNull Player player) {
        this.removeBlindness(player);
        this.lastNotification.remove(player.getUniqueId());
        this.appliedBlindness.remove(player.getUniqueId());
    }

    private void refreshPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (this.frozen) {
                this.applyPlayerFreeze(player);
                continue;
            }
            this.releasePlayer(player);
        }
    }

    private void sendFreezeTitle(Player player) {
        player.sendTitle(this.titleText, this.subtitleText, this.titleFadeIn, this.titleStay, this.titleFadeOut);
    }

    private void applyBlindness(Player player) {
        PotionEffect active;
        if (!this.blindnessEnabled) {
            return;
        }
        UUID uuid = player.getUniqueId();
        boolean firstApplication = this.appliedBlindness.add(uuid);
        if (firstApplication && (active = player.getPotionEffect(PotionEffectType.BLINDNESS)) != null) {
            this.priorBlindness.put(uuid, this.cloneEffect(active));
        }
        PotionEffect freezeEffect = new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, this.blindnessAmplifier, this.blindnessAmbient, this.blindnessParticles);
        player.addPotionEffect(freezeEffect);
    }

    private void removeBlindness(Player player) {
        if (!this.blindnessEnabled) {
            return;
        }
        UUID uuid = player.getUniqueId();
        PotionEffect prior = this.priorBlindness.remove(uuid);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        if (prior != null) {
            player.addPotionEffect(prior);
        }
    }

    private PotionEffect cloneEffect(@NotNull PotionEffect effect) {
        return new PotionEffect(effect.getType(), Math.max(effect.getDuration(), 1), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles());
    }

    public void notifyBlocked(Player player, String configPath) {
        if (player == null) {
            return;
        }
        if (this.silentNotifications) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = this.lastNotification.get(player.getUniqueId());
        if (this.notifyCooldownMs > 0L && last != null && now - last < this.notifyCooldownMs) {
            return;
        }
        this.lastNotification.put(player.getUniqueId(), now);
        Runnable task = () -> this.messagesManager.sendConfigMessage((CommandSender)player, configPath, true, null);
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask((Plugin)this.plugin, task);
        }
    }

    public boolean shouldCancelPlayer(Player player) {
        return this.frozen && player != null && !player.hasPermission("voiidcountdowntimer.freeze.bypass");
    }

    public boolean isSilentNotifications() {
        return this.silentNotifications;
    }

    public void handlePlayerJoin(Player player) {
        if (player == null) {
            return;
        }
        if (this.shouldCancelPlayer(player)) {
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.applyPlayerFreeze(player));
        }
    }

    public void handlePlayerQuit(Player player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        this.lastNotification.remove(uuid);
        this.priorBlindness.remove(uuid);
        this.appliedBlindness.remove(uuid);
        this.lastMusicPlay.remove(uuid);
    }

    public void handleTeleport(Player player, PlayerTeleportEvent event) {
        if (!this.shouldCancelPlayer(player)) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from != null && to != null && from.distanceSquared(to) > 0.0) {
            event.setCancelled(true);
            this.notifyBlocked(player, "Messages.freezeBlockedAction");
        }
    }

    private void freezeAllMobs() {
        this.mobAiSnapshot.clear();
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> Bukkit.getWorlds().forEach(world -> world.getLivingEntities().forEach(entity -> {
            if (entity instanceof Mob) {
                Mob mob = (Mob)entity;
                this.mobAiSnapshot.put(mob.getUniqueId(), mob.hasAI());
                mob.setAI(false);
            }
        })));
    }

    public void handleMobSpawn(@NotNull Mob mob) {
        if (!this.frozen || !this.mobsFrozen) {
            return;
        }
        this.mobAiSnapshot.put(mob.getUniqueId(), mob.hasAI());
        mob.setAI(false);
    }

    private void unfreezeAllMobs() {
        if (this.mobAiSnapshot.isEmpty()) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            this.mobAiSnapshot.forEach((uuid, hadAi) -> {
                Mob mob = this.findMob((UUID)uuid);
                if (mob != null) {
                    mob.setAI(hadAi.booleanValue());
                }
            });
            this.mobAiSnapshot.clear();
        });
    }

    private Mob findMob(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!uuid.equals(entity.getUniqueId()) || !(entity instanceof Mob)) continue;
                return (Mob)entity;
            }
        }
        return null;
    }

    public Collection<UUID> getFrozenMobIds() {
        return Collections.unmodifiableSet(new HashSet<UUID>(this.mobAiSnapshot.keySet()));
    }

    private void restartTitleTask() {
        this.stopTitleTask();
        this.titleTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            if (!this.frozen) {
                this.stopTitleTask();
                return;
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!this.shouldCancelPlayer(player)) continue;
                this.sendFreezeTitle(player);
            }
        }, (long)this.titleKeepaliveTicks, (long)this.titleKeepaliveTicks);
    }

    private void stopTitleTask() {
        if (this.titleTask != null) {
            try {
                this.titleTask.cancel();
            }
            catch (Exception exception) {
                // empty catch block
            }
            this.titleTask = null;
        }
    }

    private void lockWorldsToMidnight() {
        for (World world : Bukkit.getWorlds()) {
            try {
                if (!this.prevDaylight.containsKey(world)) {
                    Boolean current = (Boolean)world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE);
                    this.prevDaylight.put(world, current != null ? current : Boolean.TRUE);
                }
            }
            catch (Throwable ignored) {
                this.prevDaylight.put(world, Boolean.TRUE);
            }
            if (!this.prevWorldTime.containsKey(world)) {
                this.prevWorldTime.put(world, world.getTime());
            }
            try {
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            world.setTime(18000L);
        }
        this.restartTimeTask(Math.max(1, this.titleKeepaliveTicks));
    }

    private void restoreWorldTime() {
        for (World world : Bukkit.getWorlds()) {
            Long prevTime;
            Boolean prev = this.prevDaylight.remove(world);
            if (prev != null) {
                try {
                    world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, prev);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
            if ((prevTime = this.prevWorldTime.remove(world)) == null) continue;
            world.setTime(prevTime.longValue());
        }
    }

    private void restartTimeTask(int intervalTicks) {
        this.stopTimeTask();
        this.timeTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            if (!this.frozen || !this.lockMidnight) {
                this.stopTimeTask();
                return;
            }
            for (World world : Bukkit.getWorlds()) {
                try {
                    world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                world.setTime(18000L);
            }
        }, (long)intervalTicks, (long)intervalTicks);
    }

    private void stopTimeTask() {
        if (this.timeTask != null) {
            try {
                this.timeTask.cancel();
            }
            catch (Exception exception) {
                // empty catch block
            }
            this.timeTask = null;
        }
    }

    private void restartMusicTask() {
        this.stopMusicTask();
        if (!this.musicEnabled) {
            return;
        }
        this.musicTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            if (!this.frozen) {
                this.stopMusicTask();
                return;
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!this.shouldCancelPlayer(player)) continue;
                this.playMusicFor(player);
            }
        }, 1L, Math.max(20L, (long)this.musicLoopTicks));
    }

    private void playMusicFor(Player player) {
        if (player == null) {
            return;
        }
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long minIntervalMs = Math.max(1000L, (long)this.musicLoopTicks * 50L);
        Long last = this.lastMusicPlay.get(id);
        if (last != null && now - last < minIntervalMs) {
            return;
        }
        try {
            player.stopSound(this.musicSound);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            player.playSound(player.getLocation(), this.musicSound, this.musicVolume, this.musicPitch);
        }
        catch (Throwable t) {
            try {
                Sound snd = SoundResolver.find((String)this.musicSound);
                if (snd != null) {
                    player.playSound(player.getLocation(), snd, this.musicVolume, this.musicPitch);
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        this.lastMusicPlay.put(id, now);
    }

    private void stopMusicTask() {
        if (this.musicTask != null) {
            try {
                this.musicTask.cancel();
            }
            catch (Exception exception) {
                // empty catch block
            }
            this.musicTask = null;
        }
        if (this.musicEnabled && this.musicSound != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    player.stopSound(this.musicSound);
                }
                catch (Throwable throwable) {}
            }
        }
        this.lastMusicPlay.clear();
    }

    private void killAllMobs() {
        for (World world : Bukkit.getWorlds()) {
            world.getLivingEntities().forEach(le -> {
                if (le instanceof Mob) {
                    try {
                        le.setHealth(0.0);
                    }
                    catch (Throwable t) {
                        try {
                            le.remove();
                        }
                        catch (Throwable throwable) {
                            // empty catch block
                        }
                    }
                }
            });
        }
    }

    public boolean shouldPreventMobSpawn() {
        return this.preventMobSpawn;
    }

    private void purgeProphecyBooks(Player player) {
        try {
            voiidstudios.vct.managers.SpawnBookManager sbm = VoiidCountdownTimer.getSpawnBookManager();
            if (sbm == null || player == null) {
                return;
            }
            PlayerInventory inv = player.getInventory();
            for (int i = 0; i < inv.getSize(); ++i) {
                ItemStack cur = inv.getItem(i);
                if (!sbm.isSpawnBookItem(cur)) continue;
                inv.setItem(i, null);
            }
            ItemStack off = inv.getItemInOffHand();
            if (sbm.isSpawnBookItem(off)) {
                inv.setItemInOffHand(null);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void saveFreezeState() {
        try {
            if (!this.plugin.getDataFolder().exists()) {
                this.plugin.getDataFolder().mkdirs();
            }
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("frozen", (Object)this.frozen);
            yaml.set("mobs_frozen", (Object)this.mobsFrozen);
            yaml.save(this.freezeStateFile);
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public void loadAndApplyPersistentState() {
        try {
            if (this.freezeStateFile.exists()) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration((File)this.freezeStateFile);
                boolean wasFrozen = yaml.getBoolean("frozen", false);
                boolean wasMobs = yaml.getBoolean("mobs_frozen", this.getDefaultFreezeMobs());
                if (wasFrozen) {
                    Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.freeze(wasMobs));
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public void shutdownForDisable() {
        this.saveFreezeState();
        this.stopTitleTask();
        this.stopTimeTask();
        this.stopMusicTask();
        if (this.lockMidnight) {
            this.restoreWorldTime();
        }
        this.unfreezeAllMobs();
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.releasePlayer(player);
        }
    }
}
