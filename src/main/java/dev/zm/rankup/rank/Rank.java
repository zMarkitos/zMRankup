package dev.zm.rankup.rank;

import dev.zm.rankup.requirement.Requirement;
import dev.zm.rankup.reward.Reward;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Rank {

    private final String id;
    private final String displayName;
    private final int order;
    private final int slot;
    private final int page;
    private final int listPosition;
    private final int amount;
    private final String material;
    private final Map<String, Requirement> requirementsMap;
    private final List<Requirement> requirements;
    private final List<Reward> rewards;
    private final List<String> successActions;
    private final String templateOverride;
    private final boolean useDefaultLoreRequirements;
    private final List<String> customRequirementsLore;

    public Rank(String id, String displayName, int order, int slot, int page, String material,
                Map<String, Requirement> requirementsMap, List<Reward> rewards,
                List<String> successActions, String templateOverride,
                boolean useDefaultLoreRequirements, List<String> customRequirementsLore,
                int listPosition, int amount) {
        this.id = id;
        this.displayName = displayName;
        this.order = order;
        this.slot = slot;
        this.page = page < 1 ? 1 : page;
        this.listPosition = listPosition < 1 ? order + 1 : listPosition;
        this.amount = Math.max(1, amount);
        this.material = material;
        this.requirementsMap = requirementsMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(requirementsMap);
        this.requirements = new ArrayList<>(this.requirementsMap.values());
        this.rewards = rewards == null ? Collections.emptyList() : Collections.unmodifiableList(rewards);
        this.successActions = successActions == null ? Collections.emptyList() : Collections.unmodifiableList(successActions);
        this.templateOverride = templateOverride;
        this.useDefaultLoreRequirements = useDefaultLoreRequirements;
        this.customRequirementsLore = customRequirementsLore == null ? Collections.emptyList() : Collections.unmodifiableList(customRequirementsLore);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getOrder() {
        return order;
    }

    public int getSlot() {
        return slot;
    }

    public int getPage() {
        return page;
    }

    public int getListPosition() {
        return listPosition;
    }

    public int getAmount() {
        return amount;
    }

    public String getMaterial() {
        return material;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public Map<String, Requirement> getRequirementsMap() {
        return requirementsMap;
    }

    public boolean isUseDefaultLoreRequirements() {
        return useDefaultLoreRequirements;
    }

    public List<String> getCustomRequirementsLore() {
        return customRequirementsLore;
    }

    public List<Reward> getRewards() {
        return rewards;
    }

    public List<String> getSuccessActions() {
        return successActions;
    }

    public String getTemplateOverride() {
        return templateOverride;
    }

    public boolean isAvailable(Player player) {
        for (Requirement req : requirements) {
            if (!req.check(player)) return false;
        }
        return true;
    }

    public RankProgress getProgress(Player player) {
        Map<Requirement, Double> reqProgress = new LinkedHashMap<>();
        List<Requirement> missing = new ArrayList<>();
        double totalPercentage = 0;

        for (Requirement req : requirements) {
            double percentage = req.getProgressPercentage(player);
            reqProgress.put(req, percentage);
            
            if (percentage < 1.0) {
                missing.add(req);
            }
            totalPercentage += percentage;
        }

        double overall = requirements.isEmpty() ? 1.0 : totalPercentage / requirements.size();
        return new RankProgress(overall, reqProgress, missing, missing.isEmpty());
    }
}
