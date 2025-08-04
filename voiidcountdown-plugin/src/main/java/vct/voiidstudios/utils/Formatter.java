package vct.voiidstudios.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import vct.voiidstudios.VoiidCountdownTimer;

public enum Formatter {
    MINIMESSAGE(
            (plugin, player, text) -> MiniMessage.miniMessage().deserialize(text),
            "MiniMessage"
    ),
    LEGACY(
            (plugin, player, text) -> (Component) getLEGACY().deserialize(text),
            "Legacy"
    ),
    UNIVERSAL(
            (plugin, player, text) -> {
                final String legacy = getHEX().serialize(getLEGACY().deserialize(text));
                final Component component = (Component) getHEX().deserialize(legacy);
                String miniSerialized = MiniMessage.miniMessage().serialize(component);
                String cleaned = miniSerialized.replace("\\<", "<").replace("\\", "");
                return MiniMessage.miniMessage().deserialize(cleaned);
            },
            "Universal"
    );


    private final TriFunction<VoiidCountdownTimer, CommandSender, String, Component> formatter;
    private final String name;

    private final static LegacyComponentSerializer LEGACYCOMP = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .useUnusualXRepeatedCharacterHexFormat()
            .hexColors()
            .build();

    private final static LegacyComponentSerializer HEXCOMP = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .build();

    private static LegacyComponentSerializer getHEX() {
        return HEXCOMP;
    }

    private static LegacyComponentSerializer getLEGACY() {
        return LEGACYCOMP;
    }

    Formatter(TriFunction<VoiidCountdownTimer, CommandSender, String, Component> formatter, String name) {
        this.formatter = formatter;
        this.name = name;
    }

    /**
     * Apply formatting to a string
     *
     * @param text the string to format
     * @return the formatted string
     */
    public Component format(@NotNull VoiidCountdownTimer plugin, @NotNull CommandSender audience, @NotNull String text) {
        return formatter.apply(plugin, audience, text);
    }
}
