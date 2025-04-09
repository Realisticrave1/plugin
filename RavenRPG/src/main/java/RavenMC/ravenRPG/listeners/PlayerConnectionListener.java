package RavenMC.ravenRPG.listeners;

import RavenMC.ravenRPG.RavenRPG;
import RavenMC.ravenRPG.Managers.PlayerData;
import RavenMC.ravenRPG.Managers.Race;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerConnectionListener implements Listener {

    private final RavenRPG plugin;

    public PlayerConnectionListener(RavenRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load player data or create default data
        if (!plugin.getPlayerManager().hasPlayerData(player.getUniqueId())) {
            plugin.getPlayerManager().createDefaultPlayerData(player.getUniqueId());
        } else {
            // Ensure player data is loaded into cache
            plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        }

        // Update player's nametag
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            try {
                // We run this a tick later to ensure all data is loaded
                plugin.getNametagManager().updatePlayerNametag(player);
            } catch (Exception e) {
                plugin.getLogger().warning("Error updating nametag for " + player.getName() + ": " + e.getMessage());
            }
        }, 5L); // 5 tick delay (1/4 second)

        // Send welcome message
        final PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());

        // Delayed message to ensure it's seen after server join messages
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    // Welcome message
                    player.sendMessage(ChatColor.GOLD + "=== Welcome to RavenRPG ===");

                    // Show player status
                    String raceName = playerData.getRace();
                    int level = calculateTotalLevel(playerData);
                    double balance = playerData.getBalance();

                    player.sendMessage(ChatColor.YELLOW + "Your race: " + ChatColor.WHITE +
                            formatRaceName(raceName));
                    player.sendMessage(ChatColor.YELLOW + "Your level: " + ChatColor.WHITE + level);
                    player.sendMessage(ChatColor.YELLOW + "Your balance: " + ChatColor.GREEN +
                            plugin.getEconomyProvider().format(balance));

                    // Check if player has a raven
                    if (playerData.getRaven() != null) {
                        player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GREEN + "/raven summon" +
                                ChatColor.YELLOW + " to call your raven.");
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "You don't have a raven yet. Use " +
                                ChatColor.GREEN + "/raven summon" + ChatColor.YELLOW + " to get one.");
                    }
                }
            }
        }.runTaskLater(plugin, 40L); // 2 second delay
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Save player data
        plugin.getPlayerManager().savePlayerData(player.getUniqueId());

        // Dismiss raven if present
        plugin.getRavenManager().destroyRaven(player.getUniqueId());

        // Remove from cache after saving
        plugin.getPlayerManager().removeFromCache(player.getUniqueId());
    }

    private int calculateTotalLevel(PlayerData playerData) {
        int totalLevel = 0;

        // Sum all skill levels
        for (int level : playerData.getSkillLevels().values()) {
            totalLevel += level;
        }

        // Add raven level if present
        if (playerData.getRaven() != null) {
            totalLevel += playerData.getRaven().getLevel();
        }

        return totalLevel;
    }

    private String formatRaceName(String raceName) {
        // Capitalize race name
        if (raceName == null || raceName.isEmpty()) {
            return "Human";
        }

        try {
            // Safely retrieve race name using RaceManager
            Race race = plugin.getRaceManager().getRaces().get(raceName.toLowerCase());
            return race != null ? race.getName() :
                    raceName.substring(0, 1).toUpperCase() + raceName.substring(1).toLowerCase();
        } catch (Exception e) {
            // Fallback to simple capitalization
            return raceName.substring(0, 1).toUpperCase() + raceName.substring(1).toLowerCase();
        }
    }
}