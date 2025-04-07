package RavenMC.ravenRPG.Managers;

import RavenMC.ravenRPG.RavenRPG;
import RavenMC.ravenRPG.Managers.PlayerRaven;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {

    private final RavenRPG plugin;
    private final Map<UUID, PlayerData> playerDataCache;
    private final File dataFolder;

    public PlayerManager(RavenRPG plugin) {
        this.plugin = plugin;
        this.playerDataCache = new ConcurrentHashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");

        // Create data folder if it doesn't exist
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public PlayerData getPlayerData(UUID playerId) {
        // Check cache first
        if (playerDataCache.containsKey(playerId)) {
            return playerDataCache.get(playerId);
        }

        // Load from file
        PlayerData data = loadPlayerData(playerId);
        if (data != null) {
            playerDataCache.put(playerId, data);
            return data;
        }

        // Create new data if not found
        return createDefaultPlayerData(playerId);
    }

    public boolean hasPlayerData(UUID playerId) {
        return playerDataCache.containsKey(playerId) || getPlayerDataFile(playerId).exists();
    }

    private File getPlayerDataFile(UUID playerId) {
        return new File(dataFolder, playerId.toString() + ".yml");
    }

    private PlayerData loadPlayerData(UUID playerId) {
        File file = getPlayerDataFile(playerId);
        if (!file.exists()) {
            return null;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            // Load basic player data
            double balance = config.getDouble("economy.balance", 0);
            String race = config.getString("rpg.race", "human");
            int mana = config.getInt("rpg.mana", 100);
            int maxMana = config.getInt("rpg.maxMana", 100);

            // Load skills
            Map<String, Integer> skillLevels = new HashMap<>();
            Map<String, Integer> skillXP = new HashMap<>();

            if (config.contains("rpg.skills")) {
                for (String skillKey : config.getConfigurationSection("rpg.skills").getKeys(false)) {
                    int level = config.getInt("rpg.skills." + skillKey + ".level", 1);
                    int xp = config.getInt("rpg.skills." + skillKey + ".xp", 0);

                    skillLevels.put(skillKey, level);
                    skillXP.put(skillKey, xp);
                }
            }

            // Load raven data
            PlayerRaven raven = null;
            if (config.contains("raven")) {
                String type = config.getString("raven.type", "default");
                int level = config.getInt("raven.level", 1);
                int xp = config.getInt("raven.xp", 0);
                int color = config.getInt("raven.color", 0);

                raven = new PlayerRaven(type, level, xp, color);
            }

            // Create and return player data
            PlayerData playerData = new PlayerData(playerId);
            playerData.setBalance(balance);
            playerData.setRace(race);
            playerData.setMana(mana);
            playerData.setMaxMana(maxMana);
            playerData.setSkillLevels(skillLevels);
            playerData.setSkillXP(skillXP);
            playerData.setRaven(raven);

            return playerData;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load player data for " + playerId + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public PlayerData createDefaultPlayerData(UUID playerId) {
        PlayerData playerData = new PlayerData(playerId);

        // Set default values
        playerData.setBalance(plugin.getConfig().getDouble("economy.startingBalance", 0));
        playerData.setRace("human");
        playerData.setMana(100);
        playerData.setMaxMana(100);

        // Initialize default skills
        for (String skillName : plugin.getSkillManager().getSkills().keySet()) {
            playerData.setSkillLevel(skillName, 1);
            playerData.setSkillXP(skillName, 0);
        }

        // Create default raven
        PlayerRaven raven = new PlayerRaven("default", 1, 0, 0); // Black color (0)
        playerData.setRaven(raven);

        // Cache and save
        playerDataCache.put(playerId, playerData);
        savePlayerData(playerId, playerData);

        return playerData;
    }

    public boolean savePlayerData(UUID playerId) {
        PlayerData data = playerDataCache.get(playerId);
        if (data == null) {
            return false;
        }

        return savePlayerData(playerId, data);
    }

    public boolean savePlayerData(UUID playerId, PlayerData data) {
        // Update cache
        playerDataCache.put(playerId, data);

        // Save to file
        File file = getPlayerDataFile(playerId);
        YamlConfiguration config = new YamlConfiguration();

        // Save basic player data
        config.set("economy.balance", data.getBalance());
        config.set("rpg.race", data.getRace());
        config.set("rpg.mana", data.getMana());
        config.set("rpg.maxMana", data.getMaxMana());

        // Save skills
        for (Map.Entry<String, Integer> entry : data.getSkillLevels().entrySet()) {
            String skillName = entry.getKey();
            int level = entry.getValue();
            int xp = data.getSkillXP(skillName);

            config.set("rpg.skills." + skillName + ".level", level);
            config.set("rpg.skills." + skillName + ".xp", xp);
        }

        // Save raven data
        PlayerRaven raven = data.getRaven();
        if (raven != null) {
            config.set("raven.type", raven.getType());
            config.set("raven.level", raven.getLevel());
            config.set("raven.xp", raven.getXp());
            config.set("raven.color", raven.getColor());
        }

        try {
            config.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for " + playerId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void saveAllPlayers() {
        for (UUID playerId : playerDataCache.keySet()) {
            savePlayerData(playerId);
        }
    }

    public boolean createPlayerData(UUID playerId, PlayerData data) {
        playerDataCache.put(playerId, data);
        return savePlayerData(playerId, data);
    }

    public void removeFromCache(UUID playerId) {
        playerDataCache.remove(playerId);
    }
}

