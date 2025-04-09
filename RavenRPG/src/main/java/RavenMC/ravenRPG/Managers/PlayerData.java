package RavenMC.ravenRPG.Managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// PlayerData class to store all player information
public class PlayerData {
    private final UUID playerId;
    private double balance;
    private String race;
    private int mana;
    private int maxMana;
    private final Map<String, Integer> skillLevels;
    private final Map<String, Integer> skillXP;
    private PlayerRaven raven;

    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        this.balance = 0;
        this.race = "human";
        this.mana = 100;
        this.maxMana = 100;
        this.skillLevels = new HashMap<>();
        this.skillXP = new HashMap<>();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getRace() {
        return race;
    }

    public void setRace(String race) {
        this.race = race;
    }

    public int getMana() {
        return mana;
    }

    public void setMana(int mana) {
        this.mana = mana;
    }

    public int getMaxMana() {
        return maxMana;
    }

    public void setMaxMana(int maxMana) {
        this.maxMana = maxMana;
    }

    public int getSkillLevel(String skillName) {
        return skillLevels.getOrDefault(skillName.toLowerCase(), 1);
    }

    public void setSkillLevel(String skillName, int level) {
        skillLevels.put(skillName.toLowerCase(), level);
    }

    public int getSkillXP(String skillName) {
        return skillXP.getOrDefault(skillName.toLowerCase(), 0);
    }

    public void setSkillXP(String skillName, int xp) {
        skillXP.put(skillName.toLowerCase(), xp);
    }

    public Map<String, Integer> getSkillLevels() {
        return skillLevels;
    }

    public void setSkillLevels(Map<String, Integer> skills) {
        this.skillLevels.clear();
        if (skills != null) {
            this.skillLevels.putAll(skills);
        }
    }

    public Map<String, Integer> getSkillXP() {
        return skillXP;
    }

    public void setSkillXP(Map<String, Integer> skillXP) {
        this.skillXP.clear();
        if (skillXP != null) {
            this.skillXP.putAll(skillXP);
        }
    }

    public PlayerRaven getRaven() {
        return raven;
    }

    public void setRaven(PlayerRaven raven) {
        this.raven = raven;
    }
}
