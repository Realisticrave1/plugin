package RavenMC.ravenRPG.Managers;

import RavenMC.ravenRPG.RavenRPG;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RavenManager {

    private final RavenRPG plugin;
    private final Map<UUID, PlayerRaven> playerRavens;
    private final Map<UUID, BukkitTask> ravenTasks;
    private final Map<String, RavenType> ravenTypes;

    public RavenManager(RavenRPG plugin) {
        this.plugin = plugin;
        this.playerRavens = new ConcurrentHashMap<>();
        this.ravenTasks = new ConcurrentHashMap<>();
        this.ravenTypes = new HashMap<>();

        // Load raven types from config
        loadRavenTypes();

        // Start raven movement and effects task
        startRavenEffectsTask();
    }

    private void loadRavenTypes() {
        ConfigurationSection ravenSection = plugin.getConfig().getConfigurationSection("ravens.types");
        if (ravenSection == null) {
            plugin.getLogger().warning("No raven types found in config!");
            return;
        }

        for (String key : ravenSection.getKeys(false)) {
            ConfigurationSection typeSection = ravenSection.getConfigurationSection(key);
            if (typeSection == null) continue;

            String name = typeSection.getString("name", key);
            String description = typeSection.getString("description", "A mysterious raven");
            List<String> abilities = typeSection.getStringList("abilities");

            RavenType ravenType = new RavenType(key, name, description, abilities);
            ravenTypes.put(key, ravenType);
            plugin.getLogger().info("Loaded raven type: " + name);
        }
    }

    private void startRavenEffectsTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, PlayerRaven> entry : playerRavens.entrySet()) {
                    UUID playerId = entry.getKey();
                    PlayerRaven raven = entry.getValue();
                    Player player = Bukkit.getPlayer(playerId);

                    if (player == null || !player.isOnline()) {
                        continue;
                    }

                    // Apply passive effects based on raven type and level
                    applyRavenPassiveEffects(player, raven);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
    }

    private void applyRavenPassiveEffects(Player player, PlayerRaven raven) {
        RavenType ravenType = ravenTypes.get(raven.getType());
        if (ravenType == null) return;

        // Apply effects based on raven level and type
        int level = raven.getLevel();

        // This would be expanded based on the specific abilities of each raven type
        // For now, just an example effect
        if (level >= 5) {
            // Example: Minor regeneration for higher level ravens
            if (player.getHealth() < player.getMaxHealth() && player.getHealth() > 0) {
                player.setHealth(Math.min(player.getHealth() + 0.5, player.getMaxHealth()));
            }
        }
    }

    public void createRavenFor(Player player) {
        UUID playerId = player.getUniqueId();

        // Check if player already has a raven
        if (playerRavens.containsKey(playerId)) {
            destroyRaven(playerId);
        }

        // Get saved raven data or create default
        PlayerRaven raven = plugin.getPlayerManager().getPlayerData(playerId).getRaven();
        if (raven == null) {
            raven = new PlayerRaven("default", 1, 0, Color.BLACK.asRGB());
            plugin.getPlayerManager().getPlayerData(playerId).setRaven(raven);
        }

        playerRavens.put(playerId, raven);

        // Spawn visual representation
        spawnRavenEntity(player, raven);

        player.sendMessage(ChatColor.GOLD + "Your raven has appeared!");
    }

    private void spawnRavenEntity(Player player, PlayerRaven raven) {
        // Cancel existing task if present
        if (ravenTasks.containsKey(player.getUniqueId())) {
            ravenTasks.get(player.getUniqueId()).cancel();
        }

        // Create a task for moving the raven around the player
        BukkitTask task = new BukkitRunnable() {
            private double angle = 0;
            private ArmorStand armorStand;
            private boolean initialized = false;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    if (armorStand != null) {
                        armorStand.remove();
                    }
                    return;
                }

                if (!initialized) {
                    // Create the armor stand that will represent the raven
                    armorStand = player.getWorld().spawn(player.getLocation(), ArmorStand.class);
                    armorStand.setVisible(false);
                    armorStand.setSmall(true);
                    armorStand.setGravity(false);
                    armorStand.setCustomName(ChatColor.GOLD + player.getName() + "'s Raven");
                    armorStand.setCustomNameVisible(true);

                    // Set the armor stand's helmet to a colored block representing the raven
                    // This is a simplified representation - a more complex model would use resource packs
                    org.bukkit.inventory.ItemStack head = new org.bukkit.inventory.ItemStack(Material.PLAYER_HEAD);
                    armorStand.getEquipment().setHelmet(head);

                    initialized = true;
                }

                // Calculate position around player with some oscillation for a more natural look
                double radius = 1.5;
                double height = 1.7 + Math.sin(angle * 2) * 0.2;

                angle += 0.05;
                if (angle > 2 * Math.PI) {
                    angle = 0;
                }

                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                Vector position = player.getLocation().toVector().add(new Vector(x, height, z));
                armorStand.teleport(position.toLocation(player.getWorld()).setDirection(player.getLocation().getDirection()));
            }
        }.runTaskTimer(plugin, 0L, 2L);

        ravenTasks.put(player.getUniqueId(), task);
    }

    public void destroyRaven(UUID playerId) {
        // Cancel the movement task
        if (ravenTasks.containsKey(playerId)) {
            ravenTasks.get(playerId).cancel();
            ravenTasks.remove(playerId);
        }

        // Remove the raven visual entity
        // The actual entity removal is handled in the runnable's cancel logic

        // Remove from active ravens map
        playerRavens.remove(playerId);
    }

    public void upgradeRaven(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerRaven raven = playerRavens.get(playerId);

        if (raven == null) {
            player.sendMessage(ChatColor.RED + "You don't have a raven to upgrade!");
            return;
        }

        // Calculate XP needed for next level
        int currentLevel = raven.getLevel();
        int xpNeeded = calculateXpForLevel(currentLevel + 1) - calculateXpForLevel(currentLevel);

        if (raven.getXp() >= xpNeeded) {
            // Level up the raven
            raven.setLevel(currentLevel + 1);
            raven.setXp(raven.getXp() - xpNeeded);

            // Save changes
            plugin.getPlayerManager().getPlayerData(playerId).setRaven(raven);
            plugin.getPlayerManager().savePlayerData(playerId);

            // Notify player
            player.sendMessage(ChatColor.GREEN + "Your raven has reached level " + raven.getLevel() + "!");

            // Respawn raven entity to update visuals if needed
            destroyRaven(playerId);
            spawnRavenEntity(player, raven);
        } else {
            player.sendMessage(ChatColor.RED + "Your raven needs " + (xpNeeded - raven.getXp()) +
                    " more XP to reach level " + (currentLevel + 1) + ".");
        }
    }

    private int calculateXpForLevel(int level) {
        // Simple exponential formula for XP requirements
        return 100 * (level * level);
    }

    public void addRavenXp(UUID playerId, int amount) {
        PlayerRaven raven = playerRavens.get(playerId);
        if (raven == null) return;

        raven.setXp(raven.getXp() + amount);

        // Check for level up
        int currentLevel = raven.getLevel();
        int xpForNextLevel = calculateXpForLevel(currentLevel + 1) - calculateXpForLevel(currentLevel);

        if (raven.getXp() >= xpForNextLevel) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                upgradeRaven(player);
            }
        }

        // Save changes
        plugin.getPlayerManager().getPlayerData(playerId).setRaven(raven);
        plugin.getPlayerManager().savePlayerData(playerId);
    }

    public void changeRavenColor(Player player, int colorCode) {
        UUID playerId = player.getUniqueId();
        PlayerRaven raven = playerRavens.get(playerId);

        if (raven == null) {
            player.sendMessage(ChatColor.RED + "You don't have a raven to customize!");
            return;
        }

        raven.setColor(colorCode);

        // Save changes
        plugin.getPlayerManager().getPlayerData(playerId).setRaven(raven);
        plugin.getPlayerManager().savePlayerData(playerId);

        // Respawn raven entity to update visuals
        destroyRaven(playerId);
        spawnRavenEntity(player, raven);

        player.sendMessage(ChatColor.GREEN + "Your raven's color has been changed!");
    }

    public void activateRavenAbility(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerRaven raven = playerRavens.get(playerId);

        if (raven == null) {
            player.sendMessage(ChatColor.RED + "You don't have a raven to use abilities!");
            return;
        }

        RavenType ravenType = ravenTypes.get(raven.getType());
        if (ravenType == null) {
            player.sendMessage(ChatColor.RED + "Your raven type is invalid!");
            return;
        }

        // Check if player is on cooldown
        if (isOnCooldown(playerId)) {
            player.sendMessage(ChatColor.RED + "Your raven's abilities are on cooldown!");
            return;
        }

        // Execute ability based on raven type
        boolean success = executeRavenAbility(player, raven, ravenType);

        if (success) {
            // Apply cooldown
            applyCooldown(playerId);
        }
    }

    private boolean executeRavenAbility(Player player, PlayerRaven raven, RavenType ravenType) {
        // This would be expanded based on the specific abilities of each raven type
        // For now, a simple example ability

        String type = ravenType.getId();
        int level = raven.getLevel();

        switch (type) {
            case "scout":
                // Reveal nearby players
                int range = 20 + (level * 5); // Scales with level
                player.sendMessage(ChatColor.GOLD + "Your raven scouts the area for enemies...");

                boolean foundSomeone = false;
                for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                    if (nearbyPlayer.equals(player)) continue;

                    double distance = player.getLocation().distance(nearbyPlayer.getLocation());
                    if (distance <= range) {
                        player.sendMessage(ChatColor.YELLOW + "Your raven senses " + nearbyPlayer.getName() +
                                " is " + (int) distance + " blocks away!");
                        foundSomeone = true;
                    }
                }

                if (!foundSomeone) {
                    player.sendMessage(ChatColor.YELLOW + "Your raven senses no one nearby.");
                }

                return true;

            case "guardian":
                // Temporary damage resistance
                int duration = 10 + (level * 2); // Scales with level
                player.sendMessage(ChatColor.GOLD + "Your guardian raven protects you!");

                // Apply resistance effect
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.DAMAGE_RESISTANCE,
                        duration * 20, // Duration in ticks (20 ticks = 1 second)
                        1 // Amplifier (level 1 = Resistance II)
                ));

                return true;

            case "hunter":
                // Temporary speed boost
                int speedDuration = 15 + (level * 3); // Scales with level
                player.sendMessage(ChatColor.GOLD + "Your hunter raven grants you swiftness!");

                // Apply speed effect
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SPEED,
                        speedDuration * 20, // Duration in ticks
                        1 // Amplifier (level 1 = Speed II)
                ));

                return true;

            default:
                player.sendMessage(ChatColor.RED + "Your raven has no active abilities yet.");
                return false;
        }
    }

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private boolean isOnCooldown(UUID playerId) {
        if (!cooldowns.containsKey(playerId)) {
            return false;
        }

        long lastUsed = cooldowns.get(playerId);
        long cooldownTime = 60 * 1000; // 60 seconds in milliseconds

        return System.currentTimeMillis() - lastUsed < cooldownTime;
    }

    private void applyCooldown(UUID playerId) {
        cooldowns.put(playerId, System.currentTimeMillis());
    }

    public PlayerRaven getRaven(UUID playerId) {
        return playerRavens.get(playerId);
    }

    public Map<String, RavenType> getRavenTypes() {
        return Collections.unmodifiableMap(ravenTypes);
    }

    public void changeRavenType(Player player, String type) {
        if (!ravenTypes.containsKey(type)) {
            player.sendMessage(ChatColor.RED + "Invalid raven type: " + type);
            return;
        }

        UUID playerId = player.getUniqueId();
        PlayerRaven currentRaven = playerRavens.get(playerId);

        if (currentRaven == null) {
            // Create new raven with the specified type
            PlayerRaven raven = new PlayerRaven(type, 1, 0, Color.BLACK.asRGB());
            playerRavens.put(playerId, raven);
            plugin.getPlayerManager().getPlayerData(playerId).setRaven(raven);
        } else {
            // Update existing raven
            currentRaven.setType(type);
            plugin.getPlayerManager().getPlayerData(playerId).setRaven(currentRaven);
        }

        plugin.getPlayerManager().savePlayerData(playerId);

        // Respawn raven with new type
        destroyRaven(playerId);
        createRavenFor(player);

        player.sendMessage(ChatColor.GREEN + "Your raven has transformed into a " +
                ravenTypes.get(type).getName() + "!");
    }

    public String formatRavenType(String ravenType) {
        if (ravenTypes.containsKey(ravenType)) {
            return ravenTypes.get(ravenType).getName();
        }

        // Fallback to capitalized type name
        return ravenType.substring(0, 1).toUpperCase() + ravenType.substring(1);
    }

    // RavenType class to define different raven types
    public class RavenType {
        private final String id;
        private final String name;
        private final String description;
        private final List<String> abilities;

        public RavenType(String id, String name, String description, List<String> abilities) {
            this.id = id;
            this.name = name;
            this.description = description;
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

        public List<String> getAbilities() {
            return abilities;
        }
    }
}