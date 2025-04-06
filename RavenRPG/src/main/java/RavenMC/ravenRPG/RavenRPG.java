package RavenMC.ravenRPG;

import RavenMC.ravenRPG.RavenCommand.*;
import RavenMC.ravenRPG.RavenEconomy;
import RavenMC.ravenRPG.listeners.*;
import RavenMC.ravenRPG.Managers.*;
import RavenMC.ravenRPG.Managers.RavenManager;
import RavenMC.ravenRPG.Managers.RaceManager;
import RavenMC.ravenRPG.Managers.SkillManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class RavenRPG extends JavaPlugin {

    private static RavenRPG instance;
    private RavenManager ravenManager;
    private RaceManager raceManager;
    private SkillManager skillManager;
    private PlayerManager playerManager;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private RavenEconomy economyProvider;
    private NametagManager nametagManager;

    @Override
    public void onEnable() {
        // Set instance
        instance = this;

        // Initialize config
        saveDefaultConfig();
        configManager = new ConfigManager(this);

        // Initialize database
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // Initialize managers
        playerManager = new PlayerManager(this);
        ravenManager = new RavenManager(this);
        raceManager = new RaceManager(this);
        skillManager = new SkillManager(this);

        // Register economy with Vault if present
        if (setupEconomy()) {
            getLogger().info("Successfully hooked into Vault for economy!");
        }

        // Check for dependencies
        if (getServer().getPluginManager().getPlugin("Essentials") != null) {
            getLogger().info("Found Essentials! Economy integration enabled.");
        } else {
            getLogger().warning("Essentials not found! Some economy features may not work correctly.");
        }

        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            getLogger().info("Found LuckPerms! Rank integration enabled.");
        } else {
            getLogger().warning("LuckPerms not found! Rank features will be disabled.");
        }

        // Register listeners
        registerListeners();

        // Register commands
        registerCommands();

        // Setup nametag manager
        nametagManager = new NametagManager(this);
        nametagManager.initialize();

        getLogger().info("RavenRPG has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all player data
        if (playerManager != null) {
            playerManager.saveAllPlayers();
        }

        // Close database connection
        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        getLogger().info("RavenRPG has been disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found! Economy features will be disabled.");
            return false;
        }

        // Register our economy provider
        economyProvider = new RavenEconomy(this);
        getServer().getServicesManager().register(Economy.class, economyProvider, this, ServicePriority.Highest);
        return true;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new SkillManager.SkillEventListener(this), this);
        getServer().getPluginManager().registerEvents(new RavenAbilityListener(this), this);
    }

    private void registerCommands() {
        getCommand("raven").setExecutor(new RavenCommand(this));
        getCommand("race").setExecutor(new RaceCommand(this));
        getCommand("skill").setExecutor(new SkillCommand(this));
        getCommand("rpg").setExecutor(new RPGCommand(this));
    }

    // Getters for managers
    public static RavenRPG getInstance() {
        return instance;
    }

    public RavenManager getRavenManager() {
        return ravenManager;
    }

    public RaceManager getRaceManager() {
        return raceManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public RavenEconomy getEconomyProvider() {
        return economyProvider;
    }

    public NametagManager getNametagManager() {
        return nametagManager;
    }

    // Method to reload the plugin
    public void reload() {
        // Reload configuration
        reloadConfig();
        configManager.reloadAllConfigs();

        // Reload managers
        if (nametagManager != null) {
            nametagManager.initialize();
        }

        getLogger().info("RavenRPG has been reloaded!");
    }
}