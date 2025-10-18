package voiidstudios.vct.challenges;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import voiidstudios.vct.managers.SpawnBookManager;

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
    private Listener registeredListener;
    private List<String> defaultIncompleteBookComponents = DEFAULT_INCOMPLETE_BOOK_COMPONENTS;
    private List<String> defaultCompleteBookComponents = DEFAULT_COMPLETE_BOOK_COMPONENTS;

    public ChallengeManager(VoiidCountdownTimer plugin) {
        this.plugin = plugin;
        this.progressStore = new ChallengeProgressStore(plugin);
    }

    public void reload() {
        loadChallenges();
        progressStore.load();
        progressStore.ensureChallengeKeys(challenges.keySet());
        registerListener();
        SpawnBookManager spawnBookManager = VoiidCountdownTimer.getSpawnBookManager();
        if (spawnBookManager != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                spawnBookManager.updatePlayerBook(player);
            }
        }
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
        int count = parseInt(data.get("count"), 1);
        boolean hidden = parseBoolean(data.get("hidden"), false);

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
                count,
            bookIncompleteComponents,
            bookCompleteComponents,
            hidden);
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

    public boolean isChallengeUnlocked(Challenge challenge) {
        if (challenge == null) {
            return false;
        }
        if (!challenge.isHidden()) {
            return true;
        }
        return areAllOtherChallengesCompleted(challenge);
    }

    private boolean areAllOtherChallengesCompleted(Challenge challenge) {
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
        ChallengeListener listener = new ChallengeListener(this);
        pluginManager.registerEvents(listener, plugin);
        registeredListener = listener;
    }

    public Collection<Challenge> getChallenges() {
        return Collections.unmodifiableCollection(challenges.values());
    }

    public void handleEntityKill(EntityType entityType, Player killer) {
        List<Challenge> relevant = killChallengesByEntity.get(entityType);
        if (relevant == null || relevant.isEmpty()) {
            return;
        }
        boolean updated = false;
        for (Challenge challenge : relevant) {
            if (!isChallengeUnlocked(challenge)) {
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
                killer.sendMessage(MessagesManager.getColoredMessage(VoiidCountdownTimer.prefix + ChatColor.GOLD + "Challenge completed: " + ChatColor.LIGHT_PURPLE + challenge.getName()));
            } else {
                killer.sendMessage(MessagesManager.getColoredMessage(VoiidCountdownTimer.prefix + ChatColor.GRAY + "Progress: " + ChatColor.GOLD + challenge.getName() + ChatColor.GRAY + " (" + after + "/" + challenge.getRequiredCount() + ")"));
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
        SpawnBookManager spawnBookManager = VoiidCountdownTimer.getSpawnBookManager();
        if (spawnBookManager == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            spawnBookManager.updatePlayerBook(player);
        }
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
