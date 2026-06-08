package dev.zm.rankup.rank;

import dev.zm.rankup.requirement.Requirement;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RankProgress {

    private final double overallProgress;
    private final Map<Requirement, Double> requirementProgress;
    private final List<Requirement> missingRequirements;
    private final boolean complete;

    public RankProgress(double overallProgress, Map<Requirement, Double> requirementProgress,
                        List<Requirement> missingRequirements, boolean complete) {
        this.overallProgress = overallProgress;
        this.requirementProgress = requirementProgress == null ? Collections.emptyMap() : Collections.unmodifiableMap(requirementProgress);
        this.missingRequirements = missingRequirements == null ? Collections.emptyList() : Collections.unmodifiableList(missingRequirements);
        this.complete = complete;
    }

    public double getOverallProgress() {
        return overallProgress;
    }

    public Map<Requirement, Double> getRequirementProgress() {
        return requirementProgress;
    }

    public List<Requirement> getMissingRequirements() {
        return missingRequirements;
    }

    public boolean isComplete() {
        return complete;
    }
}
