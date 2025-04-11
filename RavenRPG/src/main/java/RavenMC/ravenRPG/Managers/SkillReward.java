package RavenMC.ravenRPG.Managers;

// SkillReward class to define rewards for skill levels
public class SkillReward {
    private final String type;
    private final String effect;
    private String description;

    public SkillReward(String type, String effect) {
        this.type = type;
        this.effect = effect;
        this.description = "";
    }

    public String getType() {
        return type;
    }

    public String getEffect() {
        return effect;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
