package RavenMC.ravenRPG.Managers;

import java.util.Map;

// Skill class to define different player skills
public class Skill {
    private final String id;
    private final String name;
    private final String description;
    private final Map<Integer, SkillReward> rewards;

    public Skill(String id, String name, String description, Map<Integer, SkillReward> rewards) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rewards = rewards;
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

    public Map<Integer, SkillReward> getRewards() {
        return rewards;
    }
}
