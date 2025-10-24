package voiidstudios.vct.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.configs.model.CustomConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Loads and executes interaction triggers defined in interactions.yml.
 * Each rule is keyed by a scoreboard tag; when a player interacts with an
 * INTERACTION entity carrying that tag, the associated commands are executed.
 */
public class InteractionActionManager {
    public enum RunAs { CONSOLE, PLAYER }

    public static class Rule {
        public final String tag;
        public final List<String> commands;
        public final RunAs runAs;
        public final boolean cancelEvent;

        Rule(String tag, List<String> commands, RunAs runAs, boolean cancelEvent) {
            this.tag = tag;
            this.commands = Collections.unmodifiableList(new ArrayList<>(commands));
            this.runAs = runAs;
            this.cancelEvent = cancelEvent;
        }
    }

    private final VoiidCountdownTimer plugin;
    private final CustomConfig configFile;
    private volatile boolean enabled = true;
    private final Map<String, Rule> rulesByTag = new ConcurrentHashMap<>();
    // Dedup window to prevent double triggers when both interact events fire
    private final Map<String, Long> recentInteractions = new ConcurrentHashMap<>();
    private static final long DEDUP_WINDOW_MS = 200L;

    public InteractionActionManager(VoiidCountdownTimer plugin) {
        this.plugin = plugin;
        this.configFile = new CustomConfig("interactions.yml", plugin, null, false);
        this.configFile.registerConfig();
        reload();
    }

    public final synchronized void reload() {
        rulesByTag.clear();
        try {
            configFile.reloadConfig();
        } catch (Throwable ignored) {}
        FileConfiguration cfg = configFile.getConfig();
        ConfigurationSection root = cfg.getConfigurationSection("Interaction");
        enabled = root == null || root.getBoolean("enabled", true);

        ConfigurationSection section = root == null ? null : root.getConfigurationSection("on_player_interact");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            String tag = key;
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) { continue; }

            String runAsStr = s.getString("run_as", "CONSOLE");
            RunAs runAs;
            try {
                runAs = RunAs.valueOf(runAsStr.toUpperCase(Locale.ROOT));
            } catch (Exception ex) {
                runAs = RunAs.CONSOLE;
            }

            boolean cancelEvent = s.getBoolean("cancel_event", false);
            List<String> commands = s.getStringList("commands");
            if (commands == null) commands = Collections.emptyList();
            if (commands.isEmpty()) continue;

            Rule rule = new Rule(tag, commands, runAs, cancelEvent);
            rulesByTag.put(tag, rule);
        }
        try {
            plugin.getLogger().info("InteractionActionManager: loaded " + rulesByTag.size() + " rule(s): " + String.join(", ", rulesByTag.keySet()));
        } catch (Throwable ignored) {}
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Checks the entity's scoreboard tags and executes any matching rules.
     * Returns true if at least one rule matched and executed.
     */
    public boolean processInteraction(Player player, Entity entity, Cancellable event) {
        if (!isEnabled() || player == null || entity == null) {
            return false;
        }

        // Deduplicate rapid duplicate events (e.g., InteractEntity + InteractAtEntity in same click)
        try {
            String key = player.getUniqueId() + "|" + entity.getUniqueId();
            long now = System.currentTimeMillis();
            Long last = recentInteractions.put(key, now);
            if (last != null && (now - last) < DEDUP_WINDOW_MS) {
                return false;
            }
        } catch (Throwable ignored) {}

        Set<String> tags;
        try {
            tags = entity.getScoreboardTags();
        } catch (Throwable t) {
            tags = Collections.emptySet();
        }
        if (tags.isEmpty()) {
            return false;
        }

        boolean anyMatched = false;
        for (String tag : tags) {
            Rule rule = rulesByTag.get(tag);
            if (rule == null) continue;
            anyMatched = true;
            plugin.getLogger().info("InteractionActionManager: matched rule '" + rule.tag + "' for player " + player.getName());
            executeRule(rule, player);
            if (rule.cancelEvent && event != null) {
                try { event.setCancelled(true); } catch (Throwable ignored) {}
            }
        }
        if (!anyMatched) {
            try {
                plugin.getLogger().fine("InteractionActionManager: no matching rules for tags " + String.join(",", tags));
            } catch (Throwable ignored) {}
        }
        return anyMatched;
    }

    private void executeRule(Rule rule, Player player) {
        CommandSender sender;
        if (rule.runAs == RunAs.PLAYER) {
            sender = player;
        } else {
            ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
            sender = console != null ? console : player;
        }

        for (String raw : rule.commands) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String cmd = applyPlaceholders(raw, player);
            try {
                Bukkit.dispatchCommand(sender, stripLeadingSlash(cmd));
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Failed executing interaction command: " + cmd, ex);
            }
        }
    }

    private String applyPlaceholders(String input, Player player) {
        String out = input;
        try {
            out = out.replace("%player%", player.getName());
        } catch (Throwable ignored) {}
        return out;
    }

    private String stripLeadingSlash(String input) {
        if (input == null) return "";
        String s = input.trim();
        if (s.startsWith("/")) return s.substring(1);
        return s;
    }
}
