package RavenMC.ravenRPG.Managers;

import java.util.List;
import java.util.Collections;

public class RavenType {
    private final String id;
    private final String name;
    private final String description;
    private final List<String> abilities;

    public RavenType(String id, String name, String description, List<String> abilities) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.abilities = abilities != null ? abilities : Collections.emptyList();
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

    public List<String> getAbilities() {
        return Collections.unmodifiableList(abilities);
    }

    @Override
    public String toString() {
        return "RavenType{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", abilities=" + abilities +
                '}';
    }
}