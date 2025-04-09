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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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
    private RaceItemManager raceItemManager;

    @Override
    public void onLoad() {
        getLogger().info("Plugin is being loaded from: " + this.getClass().getProtectionDomain().getCodeSource().getLocation());
    }

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
        raceItemManager = new RaceItemManager(this);

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

        // Start item check task to ensure players always have their ability items
        startItemCheckTask();

        getLogger().info("RavenRPG has been enabled!");
        getLogger().info("Current working directory: " + System.getProperty("user.dir"));
        getLogger().info("Plugins folder: " + getDataFolder().getParentFile().getAbsolutePath());

        RaceGUI raceGUI = new RaceGUI(this);
        getServer().getPluginManager().registerEvents(raceGUI, this);
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
        getServer().getPluginManager().registerEvents(new RaceItemListener(this), this);
    }

    private void registerCommands() {
        getCommand("raven").setExecutor(new RavenCommand(this));
        getCommand("race").setExecutor(new RaceCommand(this));
        getCommand("skill").setExecutor(new SkillCommand(this));
        getCommand("rpg").setExecutor(new RPGCommand(this));
    }

    /**
     * Start a periodic task to ensure players have their ability items
     */
    private void startItemCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // Check if ability slots are empty or have wrong items
                    ItemStack primaryItem = player.getInventory().getItem(RaceItemManager.PRIMARY_ABILITY_SLOT);
                    ItemStack secondaryItem = player.getInventory().getItem(RaceItemManager.SECONDARY_ABILITY_SLOT);

                    boolean needsUpdate = false;

                    if (primaryItem == null || !raceItemManager.isAbilityItem(primaryItem)) {
                        needsUpdate = true;
                    }

                    if (secondaryItem == null || !raceItemManager.isAbilityItem(secondaryItem)) {
                        needsUpdate = true;
                    }

                    if (needsUpdate) {
                        raceItemManager.setupPlayerAbilityItems(player);
                    }
                }
            }
        }.runTaskTimer(this, 100L, 200L); // Run every 10 seconds
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

    public RaceItemManager getRaceItemManager() {
        return raceItemManager;
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

        // Update ability items for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            raceItemManager.setupPlayerAbilityItems(player);
        }

        getLogger().info("RavenRPG has been reloaded!");
    }
}