package RavenMC.ravenRPG.Managers;

import RavenMC.ravenRPG.RavenRPG;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {

    private final RavenRPG plugin;
    private FileConfiguration config;
    private File configFile;

    // Additional config files
    private final Map<String, FileConfiguration> customConfigs = new HashMap<>();
    private final Map<String, File> customConfigFiles = new HashMap<>();

    public ConfigManager(RavenRPG plugin) {
        this.plugin = plugin;

        // Setup default config
        reloadConfig();

        // Load custom configs
        loadCustomConfig("messages.yml");
        loadCustomConfig("ravens.yml");
        loadCustomConfig("races.yml");
        loadCustomConfig("skills.yml");
    }

    /**
     * Reloads the main config file
     */
    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Look for defaults in the jar
        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream));
            config.setDefaults(defaultConfig);
        }
    }

    /**
     * Gets the main config file
     * @return The main FileConfiguration
     */
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    /**
     * Saves the main config file
     */
    public void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }
        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
        }
    }

    /**
     * Saves the default config if it doesn't exist
     */
    public void saveDefaultConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
    }

    /**
     * Loads a custom config file
     * @param fileName The name of the config file
     * @return The FileConfiguration object
     */
    public FileConfiguration loadCustomConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);

        if (!configFile.exists()) {
            // Save default from resources if it exists
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
                plugin.getLogger().info("Created default " + fileName);
            } else {
                // Create empty file
                try {
                    configFile.createNewFile();
                    plugin.getLogger().info("Created empty " + fileName);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not create " + fileName, e);
                }
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Store the config
        customConfigs.put(fileName, config);
        customConfigFiles.put(fileName, configFile);

        return config;
    }

    /**
     * Gets a custom config
     * @param fileName The name of the config file
     * @return The FileConfiguration object, or null if not loaded
     */
    public FileConfiguration getCustomConfig(String fileName) {
        FileConfiguration config = customConfigs.get(fileName);

        if (config == null) {
            config = loadCustomConfig(fileName);
        }

        return config;
    }

    /**
     * Saves a custom config
     * @param fileName The name of the config file
     */
    public void saveCustomConfig(String fileName) {
        File configFile = customConfigFiles.get(fileName);
        FileConfiguration config = customConfigs.get(fileName);

        if (config == null || configFile == null) {
            return;
        }

        try {
            config.save(configFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + fileName, ex);
        }
    }

    /**
     * Reloads a custom config
     * @param fileName The name of the config file
     * @return The reloaded FileConfiguration
     */
    public FileConfiguration reloadCustomConfig(String fileName) {
        File configFile = customConfigFiles.get(fileName);

        if (configFile == null) {
            return loadCustomConfig(fileName);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        customConfigs.put(fileName, config);

        return config;
    }

    /**
     * Reloads all configs
     */
    public void reloadAllConfigs() {
        reloadConfig();

        for (String fileName : customConfigFiles.keySet()) {
            reloadCustomConfig(fileName);
        }
    }

    /**
     * Gets a message from the messages config
     * @param path The path to the message
     * @param defaultMessage The default message if not found
     * @return The formatted message
     */
    public String getMessage(String path, String defaultMessage) {
        FileConfiguration messagesConfig = getCustomConfig("messages.yml");
        String message = messagesConfig.getString(path, defaultMessage);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Gets a list of messages from the messages config
     * @param path The path to the messages
     * @return The list of formatted messages, or empty list if not found
     */
    public List<String> getMessageList(String path) {
        FileConfiguration messagesConfig = getCustomConfig("messages.yml");
        List<String> messages = messagesConfig.getStringList(path);

        messages.replaceAll(message -> ChatColor.translateAlternateColorCodes('&', message));

        return messages;
    }

    /**
     * Gets a value from the config with a default fallback
     * @param <T> The type of value
     * @param path The path to the value
     * @param defaultValue The default value if not found
     * @return The value, or defaultValue if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String path, T defaultValue) {
        Object value = getConfig().get(path);

        if (value == null) {
            return defaultValue;
        }

        try {
            return (T) value;
        } catch (ClassCastException e) {
            plugin.getLogger().warning("Invalid type for config value: " + path);
            return defaultValue;
        }
    }

    /**
     * Gets a section of the config as a Map
     * @param path The path to the section
     * @return A Map representation of the section, or empty map if not found
     */
    public Map<String, Object> getSection(String path) {
        ConfigurationSection section = getConfig().getConfigurationSection(path);

        if (section == null) {
            return new HashMap<>();
        }

        Map<String, Object> result = new HashMap<>();

        for (String key : section.getKeys(false)) {
            result.put(key, section.get(key));
        }

        return result;
    }

    /**
     * Gets a nested map from the config
     * @param path The path to the section
     * @return A nested Map representation of the section
     */
    public Map<String, Map<String, Object>> getNestedSection(String path) {
        ConfigurationSection section = getConfig().getConfigurationSection(path);

        if (section == null) {
            return new HashMap<>();
        }

        Map<String, Map<String, Object>> result = new HashMap<>();

        for (String key : section.getKeys(false)) {
            ConfigurationSection subSection = section.getConfigurationSection(key);

            if (subSection != null) {
                Map<String, Object> subMap = new HashMap<>();

                for (String subKey : subSection.getKeys(false)) {
                    subMap.put(subKey, subSection.get(subKey));
                }

                result.put(key, subMap);
            }
        }

        return result;
    }

    /**
     * Sets a value in the config and saves
     * @param path The path to set
     * @param value The value to set
     */
    public void set(String path, Object value) {
        getConfig().set(path, value);
        saveConfig();
    }

    /**
     * Sets a value in a custom config and saves
     * @param fileName The custom config file name
     * @param path The path to set
     * @param value The value to set
     */
    public void setCustom(String fileName, String path, Object value) {
        FileConfiguration config = getCustomConfig(fileName);
        config.set(path, value);
        saveCustomConfig(fileName);
    }

    /**
     * Gets a color from the config
     * @param path The path to the color
     * @param defaultColor The default color if not found
     * @return The ChatColor
     */
    public ChatColor getColor(String path, ChatColor defaultColor) {
        String colorName = getConfig().getString(path);

        if (colorName == null) {
            return defaultColor;
        }

        try {
            return ChatColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid color in config: " + colorName + " at " + path);
            return defaultColor;
        }
    }
}