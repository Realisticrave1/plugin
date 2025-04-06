package RavenMC.ravenRPG.Managers;

// This class should be in its own file: RaceAbility.java
public class RaceAbility {
    private final String id;
    private final String name;
    private final String description;
    private final int manaCost;
    private final int cooldown; // in seconds

    public RaceAbility(String id, String name, String description, int manaCost, int cooldown) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.manaCost = manaCost;
        this.cooldown = cooldown;
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

    public int getManaCost() {
        return manaCost;
    }

    public int getCooldown() {
        return cooldown;
    }
}
