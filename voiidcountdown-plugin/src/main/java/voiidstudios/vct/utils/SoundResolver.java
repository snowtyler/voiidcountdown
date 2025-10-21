package voiidstudios.vct.utils;

import org.bukkit.Sound;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 * Utility for resolving sound identifiers without relying on deprecated enum lookups.
 */
public final class SoundResolver {

    private SoundResolver() {
    }

    public static Sound find(String input) {
        if (input == null) {
            return null;
        }

        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        Sound direct = resolveByConstant(trimmed);
        if (direct != null) {
            return direct;
        }

        int colonIndex = trimmed.indexOf(':');
        if (colonIndex >= 0 && colonIndex + 1 < trimmed.length()) {
            String withoutNamespace = trimmed.substring(colonIndex + 1);
            direct = resolveByConstant(withoutNamespace);
            if (direct != null) {
                return direct;
            }
        }

        return resolveByConstant(trimmed.replace(':', '_'));
    }

    public static String toNamespacedId(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        if (trimmed.indexOf(':') >= 0 && trimmed.indexOf('.') >= 0) {
            return trimmed.toLowerCase(Locale.ROOT);
        }

        String namespace = "minecraft";
        String path = trimmed;
        if (trimmed.contains(":")) {
            int idx = trimmed.indexOf(':');
            namespace = trimmed.substring(0, idx);
            path = trimmed.substring(idx + 1);
        }

        path = path.replace(':', '.')
                .replace('_', '.')
                .replace('-', '.')
                .toLowerCase(Locale.ROOT);

        return namespace.toLowerCase(Locale.ROOT) + ":" + path;
    }

    private static Sound resolveByConstant(String candidate) {
        String enumName = toEnumName(candidate);
        if (enumName == null) {
            return null;
        }
        try {
            Field field = Sound.class.getField(enumName);
            Object value = field.get(null);
            if (value instanceof Sound sound) {
                return sound;
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return null;
    }

    private static String toEnumName(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.replace(':', '_')
                .replace('.', '_')
                .replace('-', '_')
                .trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.startsWith("minecraft_")) {
            normalized = normalized.substring("minecraft_".length());
        }
        return normalized.toUpperCase(Locale.ROOT);
    }
}
