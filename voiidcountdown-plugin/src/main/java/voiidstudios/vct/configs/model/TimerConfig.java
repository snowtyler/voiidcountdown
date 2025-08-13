package voiidstudios.vct.configs.model;

import org.bukkit.boss.BarColor;

public class TimerConfig {
    private final String id;
    private final String text;
    private final String sound;
    private final BarColor color;
    private final boolean enabled;

    public TimerConfig(String id, String text, String sound, BarColor color, boolean enabled) {
        this.id = id;
        this.text = text;
        this.sound = sound;
        this.color = color;
        this.enabled = enabled;
    }

    public String getId() { return id; }
    public String getText() { return text; }
    public String getSound() { return sound; }
    public BarColor getColor() { return color; }
    public boolean isEnabled() { return enabled; }
}
