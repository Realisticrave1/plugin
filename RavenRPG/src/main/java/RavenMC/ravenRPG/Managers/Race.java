package RavenMC.ravenRPG.Managers;

import java.util.List;

// This class should be in its own file: Race.java
public class Race {
    private final String id;
    private final String name;
    private final String description;
    private final int baseMana;
    private final int manaRegen;
    private final List<RaceAbility> abilities;

    public Race(String id, String name, String description, int baseMana, int manaRegen, List<RaceAbility> abilities) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.baseMana = baseMana;
        this.manaRegen = manaRegen;
        this.abilities = abilities;
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

    public int getBaseMana() {
        return baseMana;
    }

    public int getManaRegen() {
        return manaRegen;
    }

    public List<RaceAbility> getAbilities() {
        return abilities;
    }
}
