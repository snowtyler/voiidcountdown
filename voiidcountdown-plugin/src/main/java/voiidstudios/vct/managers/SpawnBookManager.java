package voiidstudios.vct.managers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import voiidstudios.vct.VoiidCountdownTimer;
import voiidstudios.vct.challenges.Challenge;
import voiidstudios.vct.challenges.ChallengeManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public class SpawnBookManager {
    private static final Gson GSON = new Gson();
    private static final int MAX_LINES_PER_PAGE = 13;
    private static final int MAX_CHARACTERS_PER_PAGE = 255;
    private static final int APPROX_CHARS_PER_LINE = 18;

    private final VoiidCountdownTimer plugin;
    private List<BaseComponent[]> basePages = Collections.emptyList();
    private String templateTitle;
    private String templateAuthor;
    private BookMeta.Generation templateGeneration;
    private Boolean templateResolved;

    public SpawnBookManager(VoiidCountdownTimer plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        basePages = Collections.emptyList();
        templateTitle = null;
        templateAuthor = null;
        templateGeneration = null;
        templateResolved = null;
        loadBook();

        ChallengeManager challengeManager = VoiidCountdownTimer.getChallengeManager();
        if (challengeManager != null) {
            challengeManager.refreshAllBooks();
        }
    }

    public void giveBook(Player player) {
        if (!hasTemplate() || player == null) {
            return;
        }

        ItemStack book = createPersonalizedBook(player);
        if (book == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();

        removeExistingCopies(inventory);

        Map<Integer, ItemStack> leftovers = inventory.addItem(book);
        if (!leftovers.isEmpty()) {
            leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
    }

    private void removeExistingCopies(PlayerInventory inventory) {
        if (!hasTemplate()) {
            return;
        }

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack current = inventory.getItem(slot);
            if (isSpawnBook(current)) {
                inventory.setItem(slot, null);
            }
        }

        ItemStack offHand = inventory.getItemInOffHand();
        if (isSpawnBook(offHand)) {
            inventory.setItemInOffHand(null);
        }
    }

    private void loadBook() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder; spawn book will be disabled.");
            return;
        }

        File file = new File(plugin.getDataFolder(), "spawn_book.json");
        if (!file.exists()) {
            plugin.saveResource("spawn_book.json", false);
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!parseTemplate(root)) {
                plugin.getLogger().warning("Failed to create spawn book from spawn_book.json; feature disabled until reload.");
            }
        } catch (IOException | JsonSyntaxException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not load spawn_book.json; spawn book feature disabled.", ex);
            basePages = Collections.emptyList();
        }
    }

    private boolean parseTemplate(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            plugin.getLogger().warning("spawn_book.json is empty; spawn book feature disabled.");
            return false;
        }

        if (!element.isJsonObject()) {
            plugin.getLogger().warning("spawn_book.json root must be a JSON object; spawn book feature disabled.");
            return false;
        }

        JsonObject root = element.getAsJsonObject();
        JsonObject bookData = root.has("minecraft:written_book_content")
                ? asObject(root.get("minecraft:written_book_content"))
                : root;

        if (bookData == null) {
            plugin.getLogger().warning("spawn_book.json missing minecraft:written_book_content object.");
            return false;
        }

        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) item.getItemMeta();
        if (meta == null) {
            plugin.getLogger().warning("Unable to obtain BookMeta while preparing spawn book.");
            return false;
        }

        configureTitle(bookData, meta);
        configureAuthor(bookData, meta);
        configureGeneration(bookData, meta);
        configureResolved(bookData, meta);
        configurePages(bookData, meta);

        return true;
    }

    private void configureTitle(JsonObject bookData, BookMeta meta) {
        if (!bookData.has("title")) {
            return;
        }

        JsonElement titleElement = bookData.get("title");
        JsonElement raw = extractRawElement(titleElement);
        if (raw == null) {
            return;
        }

        BaseComponent[] components = toComponents(raw, "title");
        if (components.length == 0) {
            return;
        }

        StringBuilder plain = new StringBuilder();
        for (BaseComponent component : components) {
            plain.append(component.toPlainText());
        }

        String title = plain.toString();
        if (ChatColor.stripColor(title).length() > 32) {
            plugin.getLogger().warning("Spawn book title exceeds 32 characters after stripping colors; trimming.");
            title = title.substring(0, Math.min(title.length(), 32));
        }

        meta.setTitle(title);
        templateTitle = title;
    }

    private void configureAuthor(JsonObject bookData, BookMeta meta) {
        if (!bookData.has("author")) {
            return;
        }

        try {
            String author = bookData.get("author").getAsString();
            meta.setAuthor(author);
            templateAuthor = author;
        } catch (Exception ex) {
            plugin.getLogger().warning("Spawn book author must be a string.");
        }
    }

    private void configureGeneration(JsonObject bookData, BookMeta meta) {
        if (!bookData.has("generation")) {
            return;
        }

        try {
            int value = bookData.get("generation").getAsInt();
            BookMeta.Generation generation = mapGeneration(value);
            if (generation != null) {
                meta.setGeneration(generation);
                templateGeneration = generation;
            } else {
                plugin.getLogger().warning("Spawn book generation must be between 0 and 3.");
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Spawn book generation must be an integer.");
        }
    }

    private void configureResolved(JsonObject bookData, BookMeta meta) {
        if (!bookData.has("resolved")) {
            return;
        }

        try {
            boolean resolved = bookData.get("resolved").getAsBoolean();
            Method setResolved = meta.getClass().getMethod("setResolved", boolean.class);
            setResolved.invoke(meta, resolved);
            templateResolved = resolved;
        } catch (NoSuchMethodException ignored) {
            // Older server versions do not expose setResolved; ignore silently.
        } catch (Exception ex) {
            plugin.getLogger().log(Level.FINE, "Failed to set spawn book resolved state.", ex);
        }
    }

    private void configurePages(JsonObject bookData, BookMeta meta) {
        if (!bookData.has("pages")) {
            return;
        }

        JsonArray pagesArray = asArray(bookData.get("pages"));
        if (pagesArray == null || pagesArray.size() == 0) {
            return;
        }

        List<BaseComponent[]> pages = new ArrayList<>();
        for (int index = 0; index < pagesArray.size(); index++) {
            JsonElement pageElement = pagesArray.get(index);
            JsonElement raw = extractRawElement(pageElement);
            BaseComponent[] components = toComponents(raw, "page #" + (index + 1));
            if (components.length == 0) {
                components = new BaseComponent[]{ new TextComponent("") };
            }
            pages.add(components);
        }

        if (pages.isEmpty()) {
            return;
        }

        List<String> placeholderPages = new ArrayList<>(Collections.nCopies(pages.size(), ""));
        meta.setPages(placeholderPages);
        BookMeta.Spigot spigotMeta = meta.spigot();
        for (int i = 0; i < pages.size(); i++) {
            spigotMeta.setPage(i + 1, pages.get(i));
        }

        basePages = pages;
    }

    private JsonElement extractRawElement(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("raw")) {
                return obj.get("raw");
            }
        }

        return element;
    }

    private BaseComponent[] toComponents(JsonElement element, String sourceDescription) {
        if (element == null || element.isJsonNull()) {
            return new BaseComponent[0];
        }

        try {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String text = element.getAsString();
                return deserializeLegacy(text);
            }

            if (element.isJsonObject() || element.isJsonArray()) {
                String json = GSON.toJson(element);
                return ComponentSerializer.parse(json);
            }

            plugin.getLogger().warning(String.format(Locale.ROOT,
                    "Unsupported component type in spawn book (%s).", sourceDescription));
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING,
                    String.format(Locale.ROOT,
                            "Failed to parse component for spawn book (%s).", sourceDescription), ex);
        }

        return new BaseComponent[0];
    }

    private BaseComponent[] deserializeLegacy(String text) {
        try {
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Class<?> legacySerializerClass = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");

            Object serializer;
            try {
                serializer = legacySerializerClass.getMethod("legacySection").invoke(null);
            } catch (NoSuchMethodException nsme) {
                Class<?> builderClass = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer$Builder");
                Object builder = legacySerializerClass.getMethod("builder").invoke(null);
                builderClass.getMethod("character", char.class).invoke(builder, '&');
                try {
                    builderClass.getMethod("hexColors").invoke(builder);
                } catch (NoSuchMethodException ignored) {
                }
                serializer = builderClass.getMethod("build").invoke(builder);
            }

            Object component = legacySerializerClass.getMethod("deserialize", String.class).invoke(serializer, text);
            Class<?> bungeeSerializerClass = Class.forName("net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer");
            Object bungeeSerializer = bungeeSerializerClass.getMethod("get").invoke(null);
            Object serialized = bungeeSerializerClass.getMethod("serialize", componentClass).invoke(bungeeSerializer, component);
            if (serialized instanceof BaseComponent[]) {
                return (BaseComponent[]) serialized;
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Exception ex) {
            plugin.getLogger().log(Level.FINEST, "Falling back to TextComponent legacy parsing", ex);
        }

        try {
            Method legacyMethod = TextComponent.class.getDeclaredMethod("fromLegacyText", String.class);
            Object result = legacyMethod.invoke(null, text);
            if (result instanceof BaseComponent[]) {
                return (BaseComponent[]) result;
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse legacy text; returning literal component.", ex);
        }

        return new BaseComponent[]{ new TextComponent(text) };
    }

    private JsonObject asObject(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (!element.isJsonObject()) {
            return null;
        }
        return element.getAsJsonObject();
    }

    private JsonArray asArray(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (!element.isJsonArray()) {
            return null;
        }
        return element.getAsJsonArray();
    }

    private BookMeta.Generation mapGeneration(int value) {
        switch (value) {
            case 0:
                return BookMeta.Generation.ORIGINAL;
            case 1:
                return BookMeta.Generation.COPY_OF_ORIGINAL;
            case 2:
                return BookMeta.Generation.COPY_OF_COPY;
            case 3:
                return BookMeta.Generation.TATTERED;
            default:
                return null;
        }
    }

    public void scheduleGive(Player player) {
        if (plugin == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> giveBook(player));
    }

    public void updatePlayerBook(Player player) {
        if (!hasTemplate() || player == null) {
            return;
        }

        ItemStack replacement = createPersonalizedBook(player);
        if (replacement == null) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        boolean replaced = false;

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack current = inventory.getItem(slot);
            if (isSpawnBook(current)) {
                ItemStack clone = replacement.clone();
                clone.setAmount(current.getAmount());
                inventory.setItem(slot, clone);
                replaced = true;
            }
        }

        ItemStack offHand = inventory.getItemInOffHand();
        if (isSpawnBook(offHand)) {
            ItemStack clone = replacement.clone();
            clone.setAmount(offHand.getAmount());
            inventory.setItemInOffHand(clone);
            replaced = true;
        }

        if (!replaced) {
            // No existing copies to update; do not force-give another copy here.
        }
    }

    private ItemStack createPersonalizedBook(Player player) {
        if (!hasTemplate()) {
            return null;
        }

        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) item.getItemMeta();
        if (meta == null) {
            return null;
        }

        if (templateTitle != null) {
            meta.setTitle(templateTitle);
        }
        if (templateAuthor != null) {
            meta.setAuthor(templateAuthor);
        }
        if (templateGeneration != null) {
            meta.setGeneration(templateGeneration);
        }
        if (templateResolved != null) {
            try {
                Method setResolved = meta.getClass().getMethod("setResolved", boolean.class);
                setResolved.invoke(meta, templateResolved);
            } catch (Exception ignored) {
                // Ignored; not available on older versions.
            }
        }

        List<BaseComponent[]> challengePages = buildChallengePages();
        int totalPages = basePages.size() + challengePages.size();
        if (totalPages <= 0) {
            totalPages = 1;
        }

        List<String> blankPages = new ArrayList<>(Collections.nCopies(totalPages, ""));
        meta.setPages(blankPages);

        BookMeta.Spigot spigotMeta = meta.spigot();
        for (int i = 0; i < basePages.size(); i++) {
            spigotMeta.setPage(i + 1, cloneComponents(basePages.get(i)));
        }

        for (int i = 0; i < challengePages.size(); i++) {
            spigotMeta.setPage(basePages.size() + i + 1, challengePages.get(i));
        }

        item.setItemMeta(meta);
        return item;
    }

    private List<BaseComponent[]> buildChallengePages() {
        ChallengeManager challengeManager = VoiidCountdownTimer.getChallengeManager();
        if (challengeManager == null) {
            return Collections.emptyList();
        }

        Collection<Challenge> challengeList = challengeManager.getChallenges();
        if (challengeList.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Integer> progress = challengeManager.getProgressSnapshot();

        List<BaseComponent[]> pages = new ArrayList<>();
        List<BaseComponent> currentPageComponents = new ArrayList<>();
        int linesUsed = 0;
        int charactersUsed = 0;

        List<Challenge> orderedChallenges = new ArrayList<>(challengeList);
        List<Challenge> visibleChallenges = new ArrayList<>();
        for (Challenge challenge : orderedChallenges) {
            if (challengeManager.isChallengeUnlocked(challenge)) {
                visibleChallenges.add(challenge);
            }
        }

        for (int index = 0; index < visibleChallenges.size(); index++) {
            Challenge challenge = visibleChallenges.get(index);
            int required = Math.max(1, challenge.getRequiredCount());
            int current = Math.max(0, progress.getOrDefault(challenge.getId(), 0));
            boolean completed = current >= required;
            if (completed) {
                current = required;
            }

            List<BaseComponent[]> renderedLines = renderChallengeLines(challenge, current, required, completed);
            List<BaseComponent[]> challengeLines = new ArrayList<>(renderedLines.size());
            int challengeCharacters = 0;
            int challengeLineUsage = 0;
            for (BaseComponent[] lineComponents : renderedLines) {
                BaseComponent[] lineWithBreak = appendTrailingNewline(lineComponents);
                challengeLines.add(lineWithBreak);
                challengeCharacters += getPlainTextLength(lineWithBreak);
                challengeLineUsage += estimateLineUsage(lineWithBreak);
            }

            if (!currentPageComponents.isEmpty() &&
                    (linesUsed + challengeLineUsage > MAX_LINES_PER_PAGE
                            || charactersUsed + challengeCharacters > MAX_CHARACTERS_PER_PAGE)) {
                pages.add(currentPageComponents.toArray(new BaseComponent[0]));
                currentPageComponents = new ArrayList<>();
                linesUsed = 0;
                charactersUsed = 0;
            }

            for (BaseComponent[] lineWithBreak : challengeLines) {
                int lineCharacters = getPlainTextLength(lineWithBreak);
                int lineUsage = estimateLineUsage(lineWithBreak);
                if (linesUsed + lineUsage > MAX_LINES_PER_PAGE || charactersUsed + lineCharacters > MAX_CHARACTERS_PER_PAGE) {
                    if (!currentPageComponents.isEmpty()) {
                        pages.add(currentPageComponents.toArray(new BaseComponent[0]));
                    }
                    currentPageComponents = new ArrayList<>();
                    linesUsed = 0;
                    charactersUsed = 0;
                }
                Collections.addAll(currentPageComponents, lineWithBreak);
                linesUsed += lineUsage;
                charactersUsed += lineCharacters;
            }

            boolean hasMoreChallenges = index + 1 < visibleChallenges.size();
            if (hasMoreChallenges) {
                BaseComponent[] spacerWithBreak = appendTrailingNewline(new BaseComponent[]{ new TextComponent("") });
                int spacerCharacters = getPlainTextLength(spacerWithBreak);
                int spacerUsage = estimateLineUsage(spacerWithBreak);
                if (linesUsed + spacerUsage > MAX_LINES_PER_PAGE || charactersUsed + spacerCharacters > MAX_CHARACTERS_PER_PAGE) {
                    if (!currentPageComponents.isEmpty()) {
                        pages.add(currentPageComponents.toArray(new BaseComponent[0]));
                    }
                    currentPageComponents = new ArrayList<>();
                    linesUsed = 0;
                    charactersUsed = 0;
                }
                if (linesUsed > 0) {
                    Collections.addAll(currentPageComponents, spacerWithBreak);
                    linesUsed += spacerUsage;
                    charactersUsed += spacerCharacters;
                }
            }
        }

        if (!currentPageComponents.isEmpty()) {
            pages.add(currentPageComponents.toArray(new BaseComponent[0]));
        }

        return pages;
    }

    private List<BaseComponent[]> renderChallengeLines(Challenge challenge, int current, int required, boolean completed) {
        List<String> templates = completed ? challenge.getBookCompleteComponents() : challenge.getBookIncompleteComponents();
        if (templates == null || templates.isEmpty()) {
            templates = Collections.singletonList("{\"text\":\"\"}");
        }

        int cappedCurrent = Math.min(current, required);
        int remaining = Math.max(0, required - cappedCurrent);

        Map<String, String> replacements = new HashMap<>();
        replacements.put("%name%", nullToEmpty(challenge.getName()));
        replacements.put("%description%", nullToEmpty(challenge.getDescription()));
        replacements.put("%current%", String.valueOf(cappedCurrent));
        replacements.put("%required%", String.valueOf(required));
        replacements.put("%remaining%", String.valueOf(remaining));
        replacements.put("%status%", completed ? "completed" : "incomplete");

        List<BaseComponent[]> lines = new ArrayList<>();
        for (String template : templates) {
            if (template == null || template.trim().isEmpty()) {
                continue;
            }
            BaseComponent[] components = parseComponentTemplate(template, replacements);
            lines.add(components);
        }

        if (lines.isEmpty()) {
            lines.add(new BaseComponent[]{ new TextComponent("") });
        }

        return lines;
    }

    private BaseComponent[] parseComponentTemplate(String template, Map<String, String> replacements) {
        try {
            JsonElement element = JsonParser.parseString(template);
            JsonElement populated = applyPlaceholders(element, replacements);
            String json = GSON.toJson(populated);
            return ComponentSerializer.parse(json);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING,
                    String.format(Locale.ROOT, "Failed to parse challenge book component: %s", template), ex);
            return new BaseComponent[]{ new TextComponent("") };
        }
    }

    private JsonElement applyPlaceholders(JsonElement element, Map<String, String> replacements) {
        if (element == null || element.isJsonNull()) {
            return element;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : new ArrayList<>(object.entrySet())) {
                object.add(entry.getKey(), applyPlaceholders(entry.getValue(), replacements));
            }
            return object;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                array.set(i, applyPlaceholders(array.get(i), replacements));
            }
            return array;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                value = value.replace(entry.getKey(), entry.getValue());
            }
            return new JsonPrimitive(value);
        }
        return element;
    }

    private BaseComponent[] appendTrailingNewline(BaseComponent[] components) {
        BaseComponent[] cloned = cloneComponents(components);
        BaseComponent[] result = java.util.Arrays.copyOf(cloned, cloned.length + 1);
        result[cloned.length] = new TextComponent("\n");
        return result;
    }

    private int getPlainTextLength(BaseComponent[] components) {
        String plain = toPlainText(components);
        return plain.length();
    }

    private int estimateLineUsage(BaseComponent[] components) {
        String plain = toPlainText(components);
        if (plain.endsWith("\n")) {
            plain = plain.substring(0, plain.length() - 1);
        }
        if (plain.isEmpty()) {
            return 1;
        }
        int length = plain.length();
        return Math.max(1, (length + APPROX_CHARS_PER_LINE - 1) / APPROX_CHARS_PER_LINE);
    }

    private String toPlainText(BaseComponent[] components) {
        if (components == null || components.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (BaseComponent component : components) {
            if (component != null) {
                builder.append(component.toPlainText());
            }
        }
        return builder.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    // Legacy string pagination helper removed; pagination now operates on BaseComponent lists.

    private boolean isSpawnBook(ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK) {
            return false;
        }

        if (!(item.getItemMeta() instanceof BookMeta)) {
            return false;
        }
        BookMeta meta = (BookMeta) item.getItemMeta();

        if (templateTitle != null) {
            if (!meta.hasTitle() || !templateTitle.equals(meta.getTitle())) {
                return false;
            }
        } else if (meta.hasTitle()) {
            return false;
        }

        if (templateAuthor != null) {
            if (!meta.hasAuthor() || !templateAuthor.equals(meta.getAuthor())) {
                return false;
            }
        } else if (meta.hasAuthor()) {
            return false;
        }

        return true;
    }

    private boolean hasTemplate() {
        return !basePages.isEmpty() || templateTitle != null || templateAuthor != null;
    }

    private BaseComponent[] cloneComponents(BaseComponent[] source) {
        if (source == null) {
            return new BaseComponent[]{ new TextComponent("") };
        }
        String json = ComponentSerializer.toString(source);
        return ComponentSerializer.parse(json);
    }
}
