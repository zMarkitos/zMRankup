package dev.zm.rankup.reward;

import java.util.Collections;
import java.util.List;

public class Reward {

    private final String id;
    private final String displayName;
    private final List<String> description;
    private final List<String> commands;

    public Reward(String id, String displayName, List<String> description, List<String> commands) {
        this.id = id;
        this.displayName = displayName;
        this.description = description == null ? Collections.emptyList() : Collections.unmodifiableList(description);
        this.commands = commands == null ? Collections.emptyList() : Collections.unmodifiableList(commands);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getDescription() {
        return description;
    }

    public List<String> getCommands() {
        return commands;
    }
}
