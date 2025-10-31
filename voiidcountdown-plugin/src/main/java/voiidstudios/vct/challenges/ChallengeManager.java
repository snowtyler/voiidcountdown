package voiidstudios.vct.challenges;

import com.google.gson.Gson;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.challenges.Challenge.TriggerType;
import voiidstudios.vct.listeners.challenge.ChallengeListener;
import voiidstudios.vct.managers.MessagesManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ChallengeManager {
    private static final Gson GSON = new Gson();
    private static final List<String> DEFAULT_INCOMPLETE_BOOK_COMPONENTS = Collections.unmodifiableList(
        Collections.singletonList("{\"text\":\"%description%\",\"color\":\"#FF6B6B\",\"italic\":true}"));
    private static final List<String> DEFAULT_COMPLETE_BOOK_COMPONENTS = Collections.unmodifiableList(
        Collections.singletonList("{\"text\":\"%description%\",\"color\":\"#0C11CA\",\"italic\":true}"));

    private final VoiidCountdownTimer plugin;
    private final ChallengeProgressStore progressStore;
    private final Map<String, Challenge> challenges = new LinkedHashMap<>();
    private final Map<EntityType, List<Challenge>> killChallengesByEntity = new EnumMap<>(EntityType.class);
    private final Map<Material, List<Challenge>> acquireChallengesByMaterial = new EnumMap<>(Material.class);
    private Listener registeredListener;
    private List<String> defaultIncompleteBookComponents = DEFAULT_INCOMPLETE_BOOK_COMPONENTS;
    private List<String> defaultCompleteBookComponents = DEFAULT_COMPLETE_BOOK_COMPONENTS;
    // Completion sound settings
    private boolean completionSoundEnabled = true;
    private String completionSoundName = "minecraft:ui.toast.challenge_complete";
    private float completionSoundVolume = 1.0f;
    private float completionSoundPitch = 1.0f;

    public ChallengeManager(VoiidCountdownTimer plugin) {
        this.plugin = plugin;
        this.progressStore = new ChallengeProgressStore(plugin);
    }

    public void reload() {
        loadChallenges();
        progressStore.load();
        progressStore.ensureChallengeKeys(challenges.keySet());
        registerListener();
        // No per-player spawn book updates anymore
    }

    public void shutdown() {
        progressStore.save();
        if (registeredListener != null) {
            HandlerList.unregisterAll(registeredListener);
            registeredListener = null;
        }
    }

    private void loadChallenges() {
        challenges.clear();
        killChallengesByEntity.clear();
        acquireChallengesByMaterial.clear();

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File file = new File(plugin.getDataFolder(), "challenges.yml");
        if (!file.exists()) {
            plugin.saveResource("challenges.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        defaultIncompleteBookComponents = parseBookComponentSection(config.get("formatting.book.incomplete"), DEFAULT_INCOMPLETE_BOOK_COMPONENTS);
        defaultCompleteBookComponents = parseBookComponentSection(config.get("formatting.book.complete"), DEFAULT_COMPLETE_BOOK_COMPONENTS);
        // Load completion sound settings (with sane defaults)
        completionSoundEnabled = config.getBoolean("formatting.challenge_complete_sound.enabled", true);
        String soundName = config.getString("formatting.challenge_complete_sound.sound", "minecraft:ui.toast.challenge_complete");
        completionSoundName = trimToNull(soundName) != null ? soundName : "minecraft:ui.toast.challenge_complete";
        completionSoundVolume = (float) config.getDouble("formatting.challenge_complete_sound.volume", 1.0D);
        completionSoundPitch = (float) config.getDouble("formatting.challenge_complete_sound.pitch", 1.0D);

        List<Map<?, ?>> rawList = config.getMapList("challenges");
        if (rawList == null) {
            plugin.getLogger().warning("No challenges defined in challenges.yml.");
            return;
        }

        for (Map<?, ?> entry : rawList) {
            Challenge challenge = parseChallenge(entry);
            if (challenge == null) {
                continue;
            }

            challenges.put(challenge.getId(), challenge);
            if (challenge.getTriggerType() == TriggerType.ENTITY_KILL && challenge.getTargetEntity() != null) {
                killChallengesByEntity.computeIfAbsent(challenge.getTargetEntity(), ignored -> new ArrayList<>()).add(challenge);
            } else if (challenge.getTriggerType() == TriggerType.ITEM_ACQUIRE && challenge.getTargetMaterial() != null) {
                acquireChallengesByMaterial.computeIfAbsent(challenge.getTargetMaterial(), ignored -> new ArrayList<>()).add(challenge);
            }
        }
    }

    private Challenge parseChallenge(Map<?, ?> data) {
        if (data == null) {
            return null;
        }

        String id = trimToNull(asString(data.get("id")));
        String name = trimToNull(asString(data.get("name")));
        String description = trimToNull(asString(data.get("description")));
        String triggerRaw = trimToNull(asString(data.get("trigger")));
        String targetRaw = trimToNull(asString(data.get("target")));
        String entityTag = trimToNull(asString(data.get("entity_tag")));
        if (entityTag == null) {
            entityTag = trimToNull(asString(data.get("tag")));
        }
        int count = parseInt(data.get("count"), 1);
        boolean obfuscateUntilUnlock = parseBoolean(data.get("obfuscate_until_unlock"), false);

        if (id == null || name == null || triggerRaw == null) {
            plugin.getLogger().warning("Skipping challenge with missing id, name, or trigger.");
            return null;
        }

        TriggerType trigger;
        try {
            trigger = TriggerType.valueOf(triggerRaw.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown trigger type for challenge " + id + ": " + triggerRaw);
            return null;
        }

        EntityType entityType = null;
        Material materialType = null;
        if (trigger == TriggerType.ENTITY_KILL) {
            if (targetRaw == null) {
                plugin.getLogger().warning("Challenge " + id + " is missing target.");
                return null;
            }
            try {
                entityType = EntityType.valueOf(targetRaw.toUpperCase().replace('-', '_'));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Unknown entity type for challenge " + id + ": " + targetRaw);
                return null;
            }
        } else if (trigger == TriggerType.ITEM_ACQUIRE) {
            if (targetRaw == null) {
                plugin.getLogger().warning("Challenge " + id + " is missing target.");
                return null;
            }
            try {
                materialType = Material.valueOf(targetRaw.toUpperCase().replace('-', '_'));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Unknown material type for challenge " + id + ": " + targetRaw);
                return null;
            }
        }

        List<String> bookIncompleteComponents = defaultIncompleteBookComponents;
        List<String> bookCompleteComponents = defaultCompleteBookComponents;

        Object bookFormatSection = data.get("book_format");
        if (bookFormatSection instanceof Map<?, ?>) {
            Map<?, ?> formatMap = (Map<?, ?>) bookFormatSection;
            Object incompleteRaw = formatMap.get("incomplete");
            Object completeRaw = formatMap.get("complete");
            bookIncompleteComponents = parseBookComponentSection(incompleteRaw, bookIncompleteComponents);
            bookCompleteComponents = parseBookComponentSection(completeRaw, bookCompleteComponents);
        }

        return new Challenge(id,
            name,
            description == null ? "" : description,
            trigger,
            entityType,
            materialType,
            entityTag,
            count,
            bookIncompleteComponents,
            bookCompleteComponents,
            obfuscateUntilUnlock);
    }

    private List<String> parseBookComponentSection(Object raw, List<String> fallback) {
        if (raw == null) {
            return fallback;
        }

        List<String> parsed = new ArrayList<>();

        if (raw instanceof String) {
            String value = trimToNull((String) raw);
            if (value != null) {
                parsed.add(value);
            }
        } else if (raw instanceof List<?>) {
            List<?> rawList = (List<?>) raw;
            for (Object entry : rawList) {
                if (entry instanceof String) {
                    String value = trimToNull((String) entry);
                    if (value != null) {
                        parsed.add(value);
                    }
                } else if (entry instanceof Map<?, ?>) {
                    parsed.add(GSON.toJson(entry));
                }
            }
        } else if (raw instanceof Map<?, ?>) {
            parsed.add(GSON.toJson(raw));
        }

        if (parsed.isEmpty()) {
            return fallback;
        }

        return Collections.unmodifiableList(parsed);
    }

    private boolean parseBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String trimmed = ((String) value).trim().toLowerCase(Locale.ROOT);
            if (trimmed.isEmpty()) {
                return defaultValue;
            }
            if ("true".equals(trimmed) || "yes".equals(trimmed) || "on".equals(trimmed)) {
                return true;
            }
            if ("false".equals(trimmed) || "no".equals(trimmed) || "off".equals(trimmed)) {
                return false;
            }
        }
        return defaultValue;
    }

    public boolean areAllOtherChallengesCompleted(Challenge challenge) {
        if (challenge == null) {
            return false;
        }
        for (Challenge other : challenges.values()) {
            if (other == challenge) {
                continue;
            }
            int progress = progressStore.getProgress(other.getId());
            if (progress < other.getRequiredCount()) {
                return false;
            }
        }
        return true;
    }

    private void registerListener() {
        if (registeredListener != null) {
            HandlerList.unregisterAll(registeredListener);
            registeredListener = null;
        }

        PluginManager pluginManager = plugin.getServer().getPluginManager();
        ChallengeListener listener = new ChallengeListener(this, plugin);
        pluginManager.registerEvents(listener, plugin);
        registeredListener = listener;
    }

    public Collection<Challenge> getChallenges() {
        return Collections.unmodifiableCollection(challenges.values());
    }

    public void handleEntityKill(EntityType entityType, Set<String> entityTags, Player killer) {
        List<Challenge> relevant = killChallengesByEntity.get(entityType);
        if (relevant == null || relevant.isEmpty()) {
            return;
        }
        boolean updated = false;
        for (Challenge challenge : relevant) {
            String requiredTag = challenge.getEntityTag();
            if (requiredTag != null && (entityTags == null || !entityTags.contains(requiredTag))) {
                continue;
            }
            int before = progressStore.getProgress(challenge.getId());
            if (before >= challenge.getRequiredCount()) {
                continue;
            }

            int after = progressStore.incrementProgress(challenge.getId(), 1, challenge.getRequiredCount());
            if (after <= before) {
                continue;
            }

            updated = true;
            if (after >= challenge.getRequiredCount()) {
                // Play completion sound instead of a message
                if (killer != null && completionSoundEnabled && completionSoundName != null && !completionSoundName.trim().isEmpty()) {
                    try {
                        killer.playSound(killer.getLocation(), completionSoundName, Math.max(0.0f, completionSoundVolume), completionSoundPitch);
                    } catch (Throwable ignored) { /* keep quiet if sound id invalid */ }
                }
            } else {
                if (killer != null) {
                    killer.sendMessage(MessagesManager.getColoredMessage(VoiidCountdownTimer.prefix + ChatColor.GRAY + "Progress: " + ChatColor.GOLD + challenge.getName() + ChatColor.GRAY + " (" + after + "/" + challenge.getRequiredCount() + ")"));
                }
            }
        }

        if (updated) {
            progressStore.save();
            refreshAllBooks();
        }
    }

    public void handleItemAcquire(Material material, Player player, int amount) {
        List<Challenge> relevant = acquireChallengesByMaterial.get(material);
        if (relevant == null || relevant.isEmpty()) {
            return;
        }
        boolean updated = false;
        for (Challenge challenge : relevant) {
            int before = progressStore.getProgress(challenge.getId());
            if (before >= challenge.getRequiredCount()) {
                continue;
            }

            int after = progressStore.incrementProgress(challenge.getId(), Math.max(1, amount), challenge.getRequiredCount());
            if (after <= before) {
                continue;
            }

            updated = true;
            if (after >= challenge.getRequiredCount()) {
                // Play completion sound instead of a message
                if (player != null && completionSoundEnabled && completionSoundName != null && !completionSoundName.trim().isEmpty()) {
                    try {
                        player.playSound(player.getLocation(), completionSoundName, Math.max(0.0f, completionSoundVolume), completionSoundPitch);
                    } catch (Throwable ignored) { /* keep quiet if sound id invalid */ }
                }
            } else {
                if (player != null) {
                    player.sendMessage(MessagesManager.getColoredMessage(VoiidCountdownTimer.prefix + ChatColor.GRAY + "Progress: " + ChatColor.GOLD + challenge.getName() + ChatColor.GRAY + " (" + after + "/" + challenge.getRequiredCount() + ")"));
                }
            }
        }

        if (updated) {
            progressStore.save();
            refreshAllBooks();
        }
    }

    public int getProgress(String challengeId) {
        return progressStore.getProgress(challengeId);
    }

    public Map<String, Integer> getProgressSnapshot() {
        Map<String, Integer> raw = progressStore.getAllProgress();
        Map<String, Integer> copy = new LinkedHashMap<>();
        for (String id : challenges.keySet()) {
            copy.put(id, raw.getOrDefault(id, 0));
        }
        return copy;
    }

    public void refreshAllBooks() {
        // Update all online players' prophecy books
        var sbm = VoiidCountdownTimer.getSpawnBookManager();
        if (sbm != null) {
            try { sbm.refreshAllPlayerBooks(); } catch (Throwable ignored) {}
        }

        // Update all protected lectern copies
        var lpm = VoiidCountdownTimer.getLecternProtectionManager();
        if (lpm == null || sbm == null) return;
        try {
            org.bukkit.inventory.ItemStack latest = sbm.buildCurrentProphecyBook();
            if (latest == null) return;
            for (org.bukkit.Location loc : lpm.getAllProtectedLocations()) {
                try {
                    if (loc.getWorld() == null) continue;
                    org.bukkit.block.Block block = loc.getBlock();
                    if (block == null || block.getType() != org.bukkit.Material.LECTERN) continue;
                    org.bukkit.block.BlockState state = block.getState();
                    if (!(state instanceof org.bukkit.block.Lectern)) continue;
                    sbm.refreshLecternBook((org.bukkit.block.Lectern) state, latest);
                } catch (Throwable ignoredEach) {}
            }
        } catch (Throwable ignored) {}
    }

    public Set<String> getChallengeIds() {
        return Collections.unmodifiableSet(challenges.keySet());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int parseInt(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }
}
