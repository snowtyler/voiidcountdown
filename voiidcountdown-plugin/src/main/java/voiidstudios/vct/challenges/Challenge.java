package voiidstudios.vct.challenges;

import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Challenge {
    public enum TriggerType {
        ENTITY_KILL
    }

    private final String id;
    private final String name;
    private final String description;
    private final TriggerType triggerType;
    private final EntityType targetEntity;
    private final int requiredCount;
    private final List<String> bookIncompleteComponents;
    private final List<String> bookCompleteComponents;
    private final boolean obfuscateUntilUnlock;

    public Challenge(String id,
                     String name,
                     String description,
                     TriggerType triggerType,
                     EntityType targetEntity,
                     int requiredCount,
                     List<String> bookIncompleteComponents,
                     List<String> bookCompleteComponents,
                     boolean obfuscateUntilUnlock) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.triggerType = triggerType;
        this.targetEntity = targetEntity;
        this.requiredCount = requiredCount;
        this.bookIncompleteComponents = bookIncompleteComponents == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(bookIncompleteComponents));
        this.bookCompleteComponents = bookCompleteComponents == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(bookCompleteComponents));
        this.obfuscateUntilUnlock = obfuscateUntilUnlock;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public EntityType getTargetEntity() {
        return targetEntity;
    }

    public int getRequiredCount() {
        return requiredCount;
    }

    public List<String> getBookIncompleteComponents() {
        return bookIncompleteComponents;
    }

    public List<String> getBookCompleteComponents() {
        return bookCompleteComponents;
    }

    public boolean isObfuscateUntilUnlock() {
        return obfuscateUntilUnlock;
    }
}
