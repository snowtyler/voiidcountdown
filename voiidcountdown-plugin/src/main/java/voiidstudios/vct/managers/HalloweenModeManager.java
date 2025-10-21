package voiidstudios.vct.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.configs.model.CustomConfig;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import voiidstudios.vct.api.Timer;
import voiidstudios.vct.api.VCTActions;

public class HalloweenModeManager {
    private static final String CONFIG_ROOT = "halloween_mode";
    private static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");
    private static final String DEFAULT_TIMEZONE = "America/New_York";
    private static final int MAX_TIMER_SECONDS = 3599999;
    private static final int DEFAULT_TIMER_RESYNC_TOLERANCE_SECONDS = 10;
    private static final List<DateTimeFormatter> ZONED_FORMATS = buildZonedFormats();
    private static final List<DateTimeFormatter> OFFSET_FORMATS = buildOffsetFormats();
    private static final List<DateTimeFormatter> LOCAL_DATETIME_FORMATS = buildLocalDateTimeFormats();
    private static final List<DateTimeFormatter> LOCAL_DATE_FORMATS = buildLocalDateFormats();
    private static final Pattern OFFSET_TRAILING_PATTERN = Pattern.compile("([+-]\\d{2})(:?\\d{2})?$");
    private static final Pattern ZONE_ABBREVIATION_PATTERN = Pattern.compile("\\b[A-Za-z]{3,}\\b");

    private static List<DateTimeFormatter> buildZonedFormats() {
        List<DateTimeFormatter> formats = new ArrayList<>();
        formats.add(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        formats.add(new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("EEE MMM dd HH:mm:ss z yyyy").toFormatter(Locale.US));
        return formats;
    }

    private static List<DateTimeFormatter> buildOffsetFormats() {
        List<DateTimeFormatter> formats = new ArrayList<>();
        formats.add(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        formats.add(new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy-MM-dd HH:mm:ssXXX").toFormatter(Locale.US));
        formats.add(new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy/MM/dd HH:mm:ssXXX").toFormatter(Locale.US));
        return formats;
    }

    private static List<DateTimeFormatter> buildLocalDateTimeFormats() {
        List<DateTimeFormatter> formats = new ArrayList<>();
        formats.add(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        formats.add(DateTimeFormatter.ISO_DATE_TIME);
        formats.add(new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy-MM-dd HH:mm:ss").toFormatter(Locale.US));
        formats.add(new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy/MM/dd HH:mm:ss").toFormatter(Locale.US));
        formats.add(new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("EEE MMM dd HH:mm:ss yyyy").toFormatter(Locale.US));
        return formats;
    }

    private static List<DateTimeFormatter> buildLocalDateFormats() {
        List<DateTimeFormatter> formats = new ArrayList<>();
        formats.add(DateTimeFormatter.ISO_LOCAL_DATE);
        formats.add(new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy-MM-dd").toFormatter(Locale.US));
        formats.add(new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy/MM/dd").toFormatter(Locale.US));
        return formats;
    }

    private final VoiidCountdownTimer plugin;
    private final CustomConfig configFile;
    private final CustomConfig stateFile;

    private final List<HalloweenThreshold> thresholds = new ArrayList<>();
    private final Set<String> executedThresholdIds = new LinkedHashSet<>();

    private boolean enabled;
    private ZoneId zoneId;
    private ZonedDateTime start;
    private ZonedDateTime end;
    private boolean webhookEnabled;
    private String webhookUrl;
    private String webhookUsername;
    private String webhookAvatarUrl;
    private boolean webhookMentionEveryone;

    private Boolean manualEnabledOverride;

    private boolean timerManagementEnabled;
    private String timerId;
    private boolean timerForceOverride;
    private int timerResyncToleranceSeconds;

    private String lastExecutedThresholdId;
    private Instant lastExecutedAt;

    private BukkitTask monitorTask;

    public HalloweenModeManager(VoiidCountdownTimer plugin) {
        this.plugin = plugin;
        this.configFile = new CustomConfig("halloween.yml", plugin, null, false);
        this.stateFile = new CustomConfig("halloween_state.yml", plugin, null, true);
        this.configFile.registerConfig();
        this.stateFile.registerConfig();
    }

    public synchronized void reload() {
        stopTask();
        configFile.reloadConfig();
        stateFile.reloadConfig();
        loadState();
        parseConfig();
        startTask();
        tick();
    }

    public synchronized void shutdown() {
        stopTask();
        saveState();
    }

    private void resetExecutionHistory(String reason) {
        if (executedThresholdIds.isEmpty() && lastExecutedThresholdId == null && lastExecutedAt == null) {
            return;
        }

        executedThresholdIds.clear();
        lastExecutedThresholdId = null;
        lastExecutedAt = null;
        saveState();

        if (reason != null && !reason.trim().isEmpty()) {
            plugin.getLogger().info(reason);
        }
    }

    private void startTask() {
        if (!isModeEnabled() || start == null || end == null) {
            return;
        }

        monitorTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void stopTask() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
    }

    private void tick() {
        ZoneId effectiveZone = zoneId != null ? zoneId : ZoneId.of(DEFAULT_TIMEZONE);
        tickWithZone(effectiveZone);
    }

    private void manageTimer(ZonedDateTime now) {
        TimerManager timerManager = TimerManager.getInstance();
        Timer current = timerManager.getTimer();
        boolean currentManaged = isManagedTimer(current);

        if (!timerManagementEnabled) {
            if (currentManaged) {
                timerManager.deleteTimer(null);
            }
            return;
        }

        if (!isModeEnabled() || start == null || end == null) {
            if (currentManaged) {
                timerManager.deleteTimer(null);
            }
            return;
        }

        long epochNow = now.toEpochSecond();
        long epochStart = start.toEpochSecond();
        long epochEnd = end.toEpochSecond();

        if (epochNow < epochStart || epochNow >= epochEnd) {
            if (currentManaged) {
                timerManager.deleteTimer(null);
            }
            return;
        }

        long remaining = Math.max(0L, epochEnd - epochNow);
        if (remaining <= 0L) {
            if (currentManaged) {
                timerManager.deleteTimer(null);
            }
            return;
        }

        long total = Math.max(remaining, Math.max(0L, Duration.between(start, end).getSeconds()));
        int remainingSeconds = (int) Math.min(remaining, (long) MAX_TIMER_SECONDS);
        int totalSeconds = (int) Math.min(total, (long) MAX_TIMER_SECONDS);

        if (current == null) {
            createManagedTimer(remainingSeconds, totalSeconds);
            return;
        }

        if (!currentManaged) {
            if (timerForceOverride) {
                plugin.getLogger().info(String.format("Halloween mode is replacing active timer '%s'", current.getTimerId()));
                timerManager.deleteTimer(null);
                createManagedTimer(remainingSeconds, totalSeconds);
            }
            return;
        }

        int delta = Math.abs(current.getRemainingSeconds() - remainingSeconds);
        if (delta > timerResyncToleranceSeconds) {
            current.setSeconds(remainingSeconds);
            plugin.getLogger().fine(String.format("Halloween timer resynced by %d seconds", delta));
        }

        current.overrideInitialSeconds(totalSeconds);
    }

    private void createManagedTimer(int remainingSeconds, int totalSeconds) {
        String formatted = formatSecondsAsHHHMMSS(remainingSeconds);
        Timer timer = VCTActions.createTimer(formatted, timerId);
        if (timer != null) {
            timer.overrideInitialSeconds(totalSeconds);
            plugin.getLogger().info(String.format("Halloween mode timer '%s' started with %s remaining", timerId, formatted));
        } else {
            plugin.getLogger().warning("Failed to create Halloween timer with remaining value " + formatted + ".");
        }
    }

    private boolean isManagedTimer(Timer timer) {
        if (timer == null) {
            return false;
        }
        if (timerId == null || timerId.trim().isEmpty()) {
            return false;
        }
        String currentId = timer.getTimerId();
        return currentId != null && currentId.equalsIgnoreCase(timerId);
    }

    private String formatSecondsAsHHHMMSS(int seconds) {
        int clamped = Math.max(0, Math.min(seconds, MAX_TIMER_SECONDS));
        int hours = clamped / 3600;
        int minutes = (clamped % 3600) / 60;
        int secs = clamped % 60;
        return String.format("%d:%02d:%02d", hours, minutes, secs);
    }

    private void executeThreshold(HalloweenThreshold threshold, ZonedDateTime now) {
        executedThresholdIds.add(threshold.getId());
        lastExecutedThresholdId = threshold.getId();
        lastExecutedAt = Instant.now();
        saveState();

        plugin.getLogger().info(String.format("Triggering Halloween threshold '%s' at %s", threshold.getId(), LOG_FORMATTER.format(now)));

        if (!threshold.getCommands().isEmpty()) {
            for (String command : threshold.getCommands()) {
                if (command == null || command.trim().isEmpty()) {
                    continue;
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }

        threshold.getBroadcastMessage().ifPresent(message -> Bukkit.broadcastMessage(MessagesManager.getColoredMessage(message)));
        sendWebhook(threshold);
    }

    private void sendWebhook(HalloweenThreshold threshold) {
        String payloadContent = threshold.getWebhookContent().orElse(null);
        if (payloadContent == null || payloadContent.trim().isEmpty()) {
            return;
        }

        boolean shouldSend;
        if (threshold.getWebhookOverrideEnabled().isPresent()) {
            shouldSend = threshold.getWebhookOverrideEnabled().get();
        } else {
            shouldSend = webhookEnabled;
        }

        if (!shouldSend) {
            return;
        }

        String resolvedUrl = threshold.getWebhookOverrideUrl().orElse(webhookUrl);
        if (resolvedUrl == null || resolvedUrl.trim().isEmpty()) {
            plugin.getLogger().warning("Halloween threshold " + threshold.getId() + " tried to send a webhook but no URL is configured.");
            return;
        }

        String username = threshold.getWebhookOverrideUsername().orElse(webhookUsername);
        String avatar = threshold.getWebhookOverrideAvatar().orElse(webhookAvatarUrl);
        boolean mentionEveryone = threshold.getWebhookOverrideMentionEveryone().orElse(webhookMentionEveryone);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URI webhookUri = URI.create(resolvedUrl.trim());
                URL url = webhookUri.toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setDoOutput(true);

                String jsonPayload = buildWebhookPayload(payloadContent, username, avatar, mentionEveryone);

                try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                    writer.write(jsonPayload);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    plugin.getLogger().warning(String.format("Discord webhook for '%s' responded with HTTP %d", threshold.getId(), responseCode));
                }

                connection.disconnect();
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to send Discord webhook for threshold '" + threshold.getId() + "': " + ex.getMessage());
            }
        });
    }

    private String buildWebhookPayload(String content, String username, String avatar, boolean mentionEveryone) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        builder.append("\"content\":").append(toJsonString(content));
        if (username != null && !username.trim().isEmpty()) {
            builder.append(',').append("\"username\":").append(toJsonString(username));
        }
        if (avatar != null && !avatar.trim().isEmpty()) {
            builder.append(',').append("\"avatar_url\":").append(toJsonString(avatar));
        }
        if (!mentionEveryone) {
            builder.append(',').append("\"allowed_mentions\":{\"parse\":[]}");
        }
        builder.append('}');
        return builder.toString();
    }

    private String toJsonString(String value) {
        String sanitized = value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return '"' + sanitized + '"';
    }

    private void parseConfig() {
        thresholds.clear();

        FileConfiguration cfg = configFile.getConfig();
        ConfigurationSection section = cfg.getConfigurationSection(CONFIG_ROOT);
        if (section == null) {
            enabled = false;
            plugin.getLogger().warning("Halloween mode configuration is missing the root '" + CONFIG_ROOT + "' section.");
            return;
        }

        enabled = section.getBoolean("enabled", false);

        String timezoneId = section.getString("timezone", DEFAULT_TIMEZONE);
        try {
            zoneId = ZoneId.of(timezoneId);
        } catch (Exception ex) {
            zoneId = ZoneId.of(DEFAULT_TIMEZONE);
            plugin.getLogger().warning("Invalid timezone '" + timezoneId + "' in halloween.yml. Falling back to " + DEFAULT_TIMEZONE + ".");
        }

        Object rawStartObj = section.get("start");
        Object rawEndObj = section.get("end");

        start = parseDate(rawStartObj);
        end = parseDate(rawEndObj);

        if (start != null) {
            plugin.getLogger().info(String.format(Locale.ROOT,
                "Halloween config parsed start '%s' -> %s (%s)",
                rawStartObj,
                DISPLAY_FORMATTER.format(start),
                start.getZone().getId()));
        }
        if (end != null) {
            plugin.getLogger().info(String.format(Locale.ROOT,
                "Halloween config parsed end '%s' -> %s (%s)",
                rawEndObj,
                DISPLAY_FORMATTER.format(end),
                end.getZone().getId()));
        }

        if (start == null || end == null) {
            enabled = false;
            plugin.getLogger().warning("Halloween mode start or end time is not set correctly. Mode disabled.");
        } else if (!start.isBefore(end)) {
            enabled = false;
            plugin.getLogger().warning("Halloween mode start must be before end. Mode disabled.");
        }

        ConfigurationSection webhookSection = section.getConfigurationSection("webhook");
        if (webhookSection != null) {
            webhookEnabled = webhookSection.getBoolean("enabled", false);
            webhookUrl = webhookSection.getString("url", "");
            webhookUsername = webhookSection.getString("username", "VCT Halloween");
            webhookAvatarUrl = webhookSection.getString("avatar_url", "");
            webhookMentionEveryone = webhookSection.getBoolean("mention_everyone", false);
        } else {
            webhookEnabled = false;
            webhookUrl = "";
            webhookUsername = "VCT Halloween";
            webhookAvatarUrl = "";
            webhookMentionEveryone = false;
        }

        ConfigurationSection timerSection = section.getConfigurationSection("timer");
        if (timerSection != null) {
            timerManagementEnabled = timerSection.getBoolean("enabled", true);
            String configuredTimerId = timerSection.getString("timer_id", "halloween");
            timerId = (configuredTimerId == null || configuredTimerId.trim().isEmpty()) ? "halloween" : configuredTimerId.trim();
            timerForceOverride = timerSection.getBoolean("force_override", true);
            int tolerance = timerSection.getInt("resync_tolerance_seconds", DEFAULT_TIMER_RESYNC_TOLERANCE_SECONDS);
            timerResyncToleranceSeconds = Math.max(0, tolerance);
        } else {
            timerManagementEnabled = false;
            timerId = "halloween";
            timerForceOverride = true;
            timerResyncToleranceSeconds = DEFAULT_TIMER_RESYNC_TOLERANCE_SECONDS;
        }

        List<Map<?, ?>> rawThresholds = section.getMapList("thresholds");
        for (Map<?, ?> raw : rawThresholds) {
            HalloweenThreshold threshold = buildThreshold(raw);
            if (threshold != null) {
                thresholds.add(threshold);
            }
        }

        thresholds.sort(Comparator.comparing(HalloweenThreshold::getActivationTime, Comparator.nullsLast(Comparator.naturalOrder())));

        Set<String> validIds = thresholds.stream().map(HalloweenThreshold::getId).collect(Collectors.toSet());
        executedThresholdIds.retainAll(validIds);

        if (lastExecutedThresholdId != null && !validIds.contains(lastExecutedThresholdId)) {
            lastExecutedThresholdId = executedThresholdIds.stream()
                    .sorted((a, b) -> compareThresholdsByTime(b, a))
                    .findFirst()
                    .orElse(null);
        }

        ZonedDateTime now = zoneId != null ? ZonedDateTime.now(zoneId) : ZonedDateTime.now();
        if (start != null && now.isBefore(start)) {
            resetExecutionHistory("Halloween execution history cleared because the current time is before the configured start window.");
        }

        saveState();
    }

    private int compareThresholdsByTime(String leftId, String rightId) {
        HalloweenThreshold left = getThresholdById(leftId).orElse(null);
        HalloweenThreshold right = getThresholdById(rightId).orElse(null);
        ZonedDateTime leftTime = left != null ? left.getActivationTime() : null;
        ZonedDateTime rightTime = right != null ? right.getActivationTime() : null;
        if (leftTime == null && rightTime == null) {
            return 0;
        }
        if (leftTime == null) {
            return -1;
        }
        if (rightTime == null) {
            return 1;
        }
        return leftTime.compareTo(rightTime);
    }

    private HalloweenThreshold buildThreshold(Map<?, ?> raw) {
        if (raw == null) {
            return null;
        }

        Object idObj = raw.get("id");
        if (!(idObj instanceof String)) {
            plugin.getLogger().warning("A halloween threshold is missing a valid 'id'. Entry skipped.");
            return null;
        }
        String id = ((String) idObj).trim();
        if (id.isEmpty()) {
            plugin.getLogger().warning("A halloween threshold has an empty 'id'. Entry skipped.");
            return null;
        }

        Object atObj = raw.get("at");
        ZonedDateTime atTime = parseDate(atObj);

        List<String> commands = new ArrayList<>();
        Object commandsObj = raw.get("commands");
        if (commandsObj instanceof List<?>) {
            for (Object entry : (List<?>) commandsObj) {
                if (entry instanceof String) {
                    commands.add(((String) entry).trim());
                }
            }
        }

        String broadcast = null;
        Object broadcastObj = raw.get("broadcast_message");
        if (broadcastObj instanceof String) {
            broadcast = ((String) broadcastObj).trim();
        }

        ConfigurationSection thresholdWebhook = null;
        Object webhookObj = raw.get("webhook");
        if (webhookObj instanceof ConfigurationSection) {
            thresholdWebhook = (ConfigurationSection) webhookObj;
        }

        Boolean webhookOverrideEnabled = null;
        String webhookOverrideUrl = null;
        String webhookOverrideUsername = null;
        String webhookOverrideAvatar = null;
        Boolean webhookOverrideMentionEveryone = null;
        String webhookContent = null;

        if (webhookObj instanceof Map<?, ?>) {
            Map<?, ?> webhookMap = (Map<?, ?>) webhookObj;
            Object enabledObj = webhookMap.get("enabled");
            if (enabledObj instanceof Boolean) {
                webhookOverrideEnabled = (Boolean) enabledObj;
            }
            Object urlObj = webhookMap.get("url");
            if (urlObj instanceof String) {
                webhookOverrideUrl = ((String) urlObj).trim();
            }
            Object usernameObj = webhookMap.get("username");
            if (usernameObj instanceof String) {
                webhookOverrideUsername = ((String) usernameObj).trim();
            }
            Object avatarObj = webhookMap.get("avatar_url");
            if (avatarObj instanceof String) {
                webhookOverrideAvatar = ((String) avatarObj).trim();
            }
            Object mentionObj = webhookMap.get("mention_everyone");
            if (mentionObj instanceof Boolean) {
                webhookOverrideMentionEveryone = (Boolean) mentionObj;
            }
            Object contentObj = webhookMap.get("content");
            if (contentObj instanceof String) {
                webhookContent = ((String) contentObj).trim();
            }
        } else if (thresholdWebhook != null) {
            webhookOverrideEnabled = thresholdWebhook.contains("enabled") ? thresholdWebhook.getBoolean("enabled") : null;
            webhookOverrideUrl = thresholdWebhook.getString("url", null);
            webhookOverrideUsername = thresholdWebhook.getString("username", null);
            webhookOverrideAvatar = thresholdWebhook.getString("avatar_url", null);
            webhookOverrideMentionEveryone = thresholdWebhook.contains("mention_everyone") ? thresholdWebhook.getBoolean("mention_everyone") : null;
            webhookContent = thresholdWebhook.getString("content", null);
        } else {
            Object contentObj = raw.get("webhook_message");
            if (contentObj instanceof String) {
                webhookContent = ((String) contentObj).trim();
            }
        }

        Object fallbackContentObj = raw.get("webhook_message");
        if (webhookContent == null && fallbackContentObj instanceof String) {
            webhookContent = ((String) fallbackContentObj).trim();
        }

        return new HalloweenThreshold(
                id,
                atTime,
                Collections.unmodifiableList(commands),
                Optional.ofNullable(broadcast).filter(s -> !s.isEmpty()),
                Optional.ofNullable(webhookContent).filter(s -> !s.isEmpty()),
                Optional.ofNullable(webhookOverrideEnabled),
                Optional.ofNullable(webhookOverrideUrl).filter(s -> !s.isEmpty()),
                Optional.ofNullable(webhookOverrideUsername).filter(s -> !s.isEmpty()),
                Optional.ofNullable(webhookOverrideAvatar).filter(s -> !s.isEmpty()),
                Optional.ofNullable(webhookOverrideMentionEveryone)
        );
    }

    private ZonedDateTime parseDate(Object raw) {
        ZoneId fallbackZone = zoneId != null ? zoneId : ZoneId.of(DEFAULT_TIMEZONE);

        if (raw == null) {
            return null;
        }

        if (raw instanceof java.util.Date) {
            java.util.Date rawDate = (java.util.Date) raw;
            Instant instant = rawDate.toInstant();
            LocalDateTime localFromUtc = LocalDateTime.ofInstant(instant, java.time.ZoneOffset.UTC);
            return localFromUtc.atZone(fallbackZone);
        }

        String sanitized = raw.toString().trim();

        ZoneId targetZone = fallbackZone;

        if (containsExplicitZoneInfo(sanitized)) {
            for (DateTimeFormatter formatter : ZONED_FORMATS) {
                try {
                    return ZonedDateTime.parse(sanitized, formatter).withZoneSameInstant(targetZone);
                } catch (DateTimeParseException ignored) {}
            }

            for (DateTimeFormatter formatter : OFFSET_FORMATS) {
                try {
                    OffsetDateTime offsetDateTime = OffsetDateTime.parse(sanitized, formatter);
                    return offsetDateTime.atZoneSameInstant(targetZone);
                } catch (DateTimeParseException ignored) {}
            }

            try {
                Instant instant = Instant.parse(sanitized);
                return instant.atZone(targetZone);
            } catch (DateTimeParseException ignored) {}
        }

        for (DateTimeFormatter formatter : LOCAL_DATETIME_FORMATS) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(sanitized, formatter);
                return localDateTime.atZone(targetZone);
            } catch (DateTimeParseException ignored) {}
        }

        for (DateTimeFormatter formatter : LOCAL_DATE_FORMATS) {
            try {
                LocalDate localDate = LocalDate.parse(sanitized, formatter);
                return localDate.atStartOfDay(targetZone);
            } catch (DateTimeParseException ignored) {}
        }

        plugin.getLogger().warning("Unable to parse datetime '" + raw + "' in halloween.yml");
        return null;
    }

    private boolean containsExplicitZoneInfo(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        String trimmed = value.trim();
        if (trimmed.endsWith("Z") || trimmed.endsWith("z")) {
            return true;
        }

        int tIndex = trimmed.indexOf('T');
        if (tIndex >= 0) {
            Matcher offsetMatcher = OFFSET_TRAILING_PATTERN.matcher(trimmed.substring(tIndex + 1));
            if (offsetMatcher.find()) {
                return true;
            }
        }

        Matcher zoneMatcher = ZONE_ABBREVIATION_PATTERN.matcher(trimmed);
        if (zoneMatcher.find()) {
            String token = zoneMatcher.group();
            if (token.length() >= 3 && token.chars().allMatch(Character::isLetter)) {
                // Allow common timezone abbreviations such as EDT, PST, etc.
                return true;
            }
        }

        return false;
    }

    private void loadState() {
        FileConfiguration cfg = stateFile.getConfig();
        executedThresholdIds.clear();
        executedThresholdIds.addAll(cfg.getStringList("executed_thresholds"));
        lastExecutedThresholdId = cfg.getString("last_executed_threshold", null);
        String lastAt = cfg.getString("last_executed_at", null);
        if (lastAt != null) {
            try {
                lastExecutedAt = Instant.parse(lastAt);
            } catch (DateTimeParseException ex) {
                lastExecutedAt = null;
            }
        } else {
            lastExecutedAt = null;
        }

        if (cfg.contains("manual_enabled_override")) {
            Object raw = cfg.get("manual_enabled_override");
            manualEnabledOverride = raw instanceof Boolean ? (Boolean) raw : null;
        } else {
            manualEnabledOverride = null;
        }
    }

    private void saveState() {
        FileConfiguration cfg = stateFile.getConfig();
        cfg.set("executed_thresholds", new ArrayList<>(executedThresholdIds));
        cfg.set("last_executed_threshold", lastExecutedThresholdId);
        cfg.set("last_executed_at", lastExecutedAt != null ? lastExecutedAt.toString() : null);
        cfg.set("manual_enabled_override", manualEnabledOverride);
        stateFile.saveConfig();
    }

    private Optional<HalloweenThreshold> getThresholdById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return thresholds.stream().filter(th -> th.getId().equalsIgnoreCase(id)).findFirst();
    }

    private boolean isModeEnabled() {
        return manualEnabledOverride != null ? manualEnabledOverride : enabled;
    }

    public synchronized Optional<Boolean> getManualEnabledOverride() {
        return Optional.ofNullable(manualEnabledOverride);
    }

    public synchronized boolean applyManualEnabledOverride(Boolean override) {
        manualEnabledOverride = override;
        saveState();
        stopTask();
        if (isModeEnabled() && start != null && end != null) {
            startTask();
        }
        ZoneId zone = zoneId != null ? zoneId : ZoneId.of(DEFAULT_TIMEZONE);
        tickWithZone(zone);
        String state = isModeEnabled() ? "enabled" : "disabled";
        String suffix = manualEnabledOverride == null ? " (following config)" : " (manual override)";
        plugin.getLogger().info("Halloween mode manually set to " + state + suffix + ".");
        return isModeEnabled();
    }

    public synchronized boolean toggleManualEnabledOverride() {
        boolean desired = !isModeEnabled();
        return applyManualEnabledOverride(desired);
    }

    public synchronized boolean clearManualEnabledOverride() {
        return applyManualEnabledOverride(null);
    }

    public synchronized Optional<HalloweenThreshold> forceNextThresholdExecution() {
        Optional<HalloweenThreshold> next = findNextPendingThreshold();
        if (!next.isPresent()) {
            return Optional.empty();
        }

        ZoneId zone = zoneId != null ? zoneId : ZoneId.of(DEFAULT_TIMEZONE);
        executeThreshold(next.get(), ZonedDateTime.now(zone));
        return next;
    }

    public synchronized Optional<String> resetToPreviousThreshold() {
        if (executedThresholdIds.isEmpty()) {
            return Optional.empty();
        }

        List<String> executed = new ArrayList<>(executedThresholdIds);
        String removedId = executed.remove(executed.size() - 1);
        executedThresholdIds.clear();
        executedThresholdIds.addAll(executed);

        lastExecutedThresholdId = executed.isEmpty() ? null : executed.get(executed.size() - 1);
        lastExecutedAt = null;
        saveState();

        plugin.getLogger().info("Halloween threshold reset: " + removedId + ". Reverted to previous state.");

        return Optional.of(removedId);
    }

    private Optional<HalloweenThreshold> findNextPendingThreshold() {
        for (HalloweenThreshold threshold : thresholds) {
            if (!executedThresholdIds.contains(threshold.getId())) {
                return Optional.of(threshold);
            }
        }
        return Optional.empty();
    }

    private void tickWithZone(ZoneId zone) {
        ZonedDateTime now = ZonedDateTime.now(zone);
        manageTimer(now);
        if (start != null && now.isBefore(start)) {
            resetExecutionHistory(null);
            return;
        }
        if (!isModeEnabled() || start == null || end == null) {
            return;
        }

        if (!now.isBefore(end)) {
            return;
        }

        for (HalloweenThreshold threshold : thresholds) {
            if (threshold.getActivationTime() == null) {
                continue;
            }
            if (executedThresholdIds.contains(threshold.getId())) {
                continue;
            }
            if (!now.isBefore(threshold.getActivationTime())) {
                executeThreshold(threshold, now);
            } else {
                break;
            }
        }
    }

    public synchronized HalloweenStatusSnapshot getStatusSnapshot() {
        ZonedDateTime now = zoneId != null ? ZonedDateTime.now(zoneId) : ZonedDateTime.now();
        List<HalloweenThreshold> executed = thresholds.stream()
                .filter(th -> executedThresholdIds.contains(th.getId()))
                .sorted(Comparator.comparing(HalloweenThreshold::getActivationTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        HalloweenThreshold lastExecuted = null;
        if (lastExecutedThresholdId != null) {
            lastExecuted = getThresholdById(lastExecutedThresholdId).orElse(null);
        }
        if (lastExecuted == null && !executed.isEmpty()) {
            lastExecuted = executed.get(executed.size() - 1);
        }

        HalloweenThreshold next = thresholds.stream()
            .filter(th -> !executedThresholdIds.contains(th.getId()))
            .min(Comparator.comparing(HalloweenThreshold::getActivationTime, Comparator.nullsLast(Comparator.naturalOrder())))
            .orElse(null);

        Timer currentTimer = TimerManager.getInstance().getTimer();
        boolean managedTimerActive = timerManagementEnabled && isManagedTimer(currentTimer);
        int managedTimerRemaining = managedTimerActive ? currentTimer.getRemainingSeconds() : -1;
        long expectedRemaining = (start != null && end != null) ? Math.max(0L, Duration.between(now, end).getSeconds()) : -1L;
        long expectedClamped = expectedRemaining >= 0 ? Math.min(expectedRemaining, (long) MAX_TIMER_SECONDS) : expectedRemaining;
        boolean timerOutOfSync = managedTimerActive && expectedClamped >= 0
            && Math.abs(managedTimerRemaining - (int) expectedClamped) > timerResyncToleranceSeconds;

        return new HalloweenStatusSnapshot(
            enabled,
            isModeEnabled(),
            manualEnabledOverride,
            zoneId,
                start,
                end,
                now,
                lastExecuted,
                lastExecutedAt != null ? ZonedDateTime.ofInstant(lastExecutedAt, zoneId != null ? zoneId : ZoneId.systemDefault()) : null,
                executed,
                next,
                new ArrayList<>(executedThresholdIds),
                thresholds.size(),
                webhookEnabled && webhookUrl != null && !webhookUrl.trim().isEmpty(),
                timerManagementEnabled,
                timerId,
                managedTimerActive,
                managedTimerRemaining,
                expectedClamped,
                timerResyncToleranceSeconds,
                timerOutOfSync
        );
    }

    public static class HalloweenStatusSnapshot {
        private final boolean enabled;
        private final boolean effectiveEnabled;
        private final Boolean manualOverride;
        private final ZoneId zoneId;
        private final ZonedDateTime start;
        private final ZonedDateTime end;
        private final ZonedDateTime now;
        private final HalloweenThreshold lastExecuted;
        private final ZonedDateTime lastExecutedAt;
        private final List<HalloweenThreshold> executedThresholds;
        private final HalloweenThreshold nextThreshold;
        private final List<String> executedThresholdIds;
        private final int totalThresholds;
        private final boolean webhookConfigured;
        private final boolean timerManagementEnabled;
        private final String timerId;
        private final boolean timerActive;
        private final int timerRemainingSeconds;
        private final long timerExpectedSeconds;
        private final int timerResyncToleranceSeconds;
        private final boolean timerOutOfSync;

        public HalloweenStatusSnapshot(boolean enabled,
                           boolean effectiveEnabled,
                           Boolean manualOverride,
                           ZoneId zoneId,
                                       ZonedDateTime start,
                                       ZonedDateTime end,
                                       ZonedDateTime now,
                                       HalloweenThreshold lastExecuted,
                                       ZonedDateTime lastExecutedAt,
                                       List<HalloweenThreshold> executedThresholds,
                                       HalloweenThreshold nextThreshold,
                                       List<String> executedThresholdIds,
                                       int totalThresholds,
                           boolean webhookConfigured,
                           boolean timerManagementEnabled,
                           String timerId,
                           boolean timerActive,
                           int timerRemainingSeconds,
                           long timerExpectedSeconds,
                           int timerResyncToleranceSeconds,
                           boolean timerOutOfSync) {
            this.enabled = enabled;
            this.zoneId = zoneId;
            this.start = start;
            this.end = end;
            this.now = now;
            this.lastExecuted = lastExecuted;
            this.lastExecutedAt = lastExecutedAt;
            this.executedThresholds = executedThresholds;
            this.nextThreshold = nextThreshold;
            this.executedThresholdIds = executedThresholdIds;
            this.totalThresholds = totalThresholds;
            this.webhookConfigured = webhookConfigured;
            this.timerManagementEnabled = timerManagementEnabled;
            this.timerId = timerId;
            this.timerActive = timerActive;
            this.timerRemainingSeconds = timerRemainingSeconds;
            this.timerExpectedSeconds = timerExpectedSeconds;
            this.timerResyncToleranceSeconds = timerResyncToleranceSeconds;
            this.timerOutOfSync = timerOutOfSync;
            this.effectiveEnabled = effectiveEnabled;
            this.manualOverride = manualOverride;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isEffectiveEnabled() {
            return effectiveEnabled;
        }

        public Optional<Boolean> getManualOverride() {
            return Optional.ofNullable(manualOverride);
        }

        public ZoneId getZoneId() {
            return zoneId;
        }

        public ZonedDateTime getStart() {
            return start;
        }

        public ZonedDateTime getEnd() {
            return end;
        }

        public ZonedDateTime getNow() {
            return now;
        }

        public HalloweenThreshold getLastExecuted() {
            return lastExecuted;
        }

        public ZonedDateTime getLastExecutedAt() {
            return lastExecutedAt;
        }

        public List<HalloweenThreshold> getExecutedThresholds() {
            return executedThresholds;
        }

        public HalloweenThreshold getNextThreshold() {
            return nextThreshold;
        }

        public List<String> getExecutedThresholdIds() {
            return executedThresholdIds;
        }

        public int getTotalThresholds() {
            return totalThresholds;
        }

        public boolean isWebhookConfigured() {
            return webhookConfigured;
        }

        public String formatDate(ZonedDateTime time) {
            if (time == null) {
                return "-";
            }
            return DISPLAY_FORMATTER.format(time);
        }

        public boolean isTimerManagementEnabled() {
            return timerManagementEnabled;
        }

        public String getTimerId() {
            return timerId != null ? timerId : "-";
        }

        public boolean isTimerActive() {
            return timerActive;
        }

        public int getTimerRemainingSeconds() {
            return timerRemainingSeconds;
        }

        public long getTimerExpectedSeconds() {
            return timerExpectedSeconds;
        }

        public boolean isTimerOutOfSync() {
            return timerOutOfSync;
        }

        public int getTimerResyncToleranceSeconds() {
            return timerResyncToleranceSeconds;
        }

        public String formatDuration(long seconds) {
            if (seconds < 0) {
                return "-";
            }
            long clamped = Math.min(seconds, MAX_TIMER_SECONDS);
            long hours = clamped / 3600;
            long minutes = (clamped % 3600) / 60;
            long secs = clamped % 60;
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        }
    }

    public static class HalloweenThreshold {
        private final String id;
        private final ZonedDateTime activationTime;
        private final List<String> commands;
        private final Optional<String> broadcastMessage;
        private final Optional<String> webhookContent;
        private final Optional<Boolean> webhookOverrideEnabled;
        private final Optional<String> webhookOverrideUrl;
        private final Optional<String> webhookOverrideUsername;
        private final Optional<String> webhookOverrideAvatar;
        private final Optional<Boolean> webhookOverrideMentionEveryone;

        public HalloweenThreshold(String id,
                                  ZonedDateTime activationTime,
                                  List<String> commands,
                                  Optional<String> broadcastMessage,
                                  Optional<String> webhookContent,
                                  Optional<Boolean> webhookOverrideEnabled,
                                  Optional<String> webhookOverrideUrl,
                                  Optional<String> webhookOverrideUsername,
                                  Optional<String> webhookOverrideAvatar,
                                  Optional<Boolean> webhookOverrideMentionEveryone) {
            this.id = id;
            this.activationTime = activationTime;
            this.commands = commands;
            this.broadcastMessage = broadcastMessage;
            this.webhookContent = webhookContent;
            this.webhookOverrideEnabled = webhookOverrideEnabled;
            this.webhookOverrideUrl = webhookOverrideUrl;
            this.webhookOverrideUsername = webhookOverrideUsername;
            this.webhookOverrideAvatar = webhookOverrideAvatar;
            this.webhookOverrideMentionEveryone = webhookOverrideMentionEveryone;
        }

        public String getId() {
            return id;
        }

        public ZonedDateTime getActivationTime() {
            return activationTime;
        }

        public List<String> getCommands() {
            return commands;
        }

        public Optional<String> getBroadcastMessage() {
            return broadcastMessage;
        }

        public Optional<String> getWebhookContent() {
            return webhookContent;
        }

        public Optional<Boolean> getWebhookOverrideEnabled() {
            return webhookOverrideEnabled;
        }

        public Optional<String> getWebhookOverrideUrl() {
            return webhookOverrideUrl;
        }

        public Optional<String> getWebhookOverrideUsername() {
            return webhookOverrideUsername;
        }

        public Optional<String> getWebhookOverrideAvatar() {
            return webhookOverrideAvatar;
        }

        public Optional<Boolean> getWebhookOverrideMentionEveryone() {
            return webhookOverrideMentionEveryone;
        }
    }
}
