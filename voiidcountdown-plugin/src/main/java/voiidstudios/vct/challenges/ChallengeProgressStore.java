package voiidstudios.vct.challenges;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import voiidstudios.vct.VoiidCountdownTimer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChallengeProgressStore {
    private final VoiidCountdownTimer plugin;
    private final File file;
    private final Map<String, Integer> progressCache = new LinkedHashMap<>();

    public ChallengeProgressStore(VoiidCountdownTimer plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "challenge_progress.yml");
    }

    public void load() {
        progressCache.clear();

        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("progress");
        if (root == null) {
            return;
        }

        for (String challengeId : root.getKeys(false)) {
            int value = root.getInt(challengeId, 0);
            if (value < 0) {
                value = 0;
            }
            progressCache.put(challengeId, value);
        }
    }

    public void save() {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, Integer> entry : progressCache.entrySet()) {
            config.set("progress." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save challenge progress: " + ex.getMessage());
        }
    }

    public int getProgress(String challengeId) {
        return progressCache.getOrDefault(challengeId, 0);
    }

    public int incrementProgress(String challengeId, int amount, int max) {
        if (amount <= 0) {
            return getProgress(challengeId);
        }

        int current = progressCache.getOrDefault(challengeId, 0);
        if (current >= max) {
            return current;
        }

        int updated = current + amount;
        if (updated > max) {
            updated = max;
        }
        progressCache.put(challengeId, updated);
        return updated;
    }

    public void ensureChallengeKeys(Collection<String> challengeIds) {
        for (String id : challengeIds) {
            progressCache.putIfAbsent(id, 0);
        }
    }

    public Map<String, Integer> getAllProgress() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(progressCache));
    }
}
