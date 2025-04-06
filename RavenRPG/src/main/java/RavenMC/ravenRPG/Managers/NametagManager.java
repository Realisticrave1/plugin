package RavenMC.ravenRPG.Managers;

import RavenMC.ravenRPG.RavenRPG;
import RavenMC.ravenRPG.Managers.PlayerData;
import RavenMC.ravenRPG.Managers.PlayerRaceChangeEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NametagManager implements Listener {

    private final RavenRPG plugin;
    private final Map<String, ChatColor> raceColors;
    private Scoreboard scoreboard;
    private boolean useLuckPerms;
    private boolean usePlaceholderAPI;

    public NametagManager(RavenRPG plugin) {
        this.plugin = plugin;
        this.raceColors = new HashMap<>();

        // Setup race colors
        setupRaceColors();

        // Check for dependencies
        useLuckPerms = Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
        usePlaceholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        if (useLuckPerms) {
            plugin.getLogger().info("Found LuckPerms! Rank display enabled.");
        } else {
            plugin.getLogger().warning("LuckPerms not found! Rank display will be disabled.");
        }

        if (usePlaceholderAPI) {
            plugin.getLogger().info("Found PlaceholderAPI! Additional placeholders enabled.");
        }

        // Register this class as an event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void setupRaceColors() {
        raceColors.put("human", ChatColor.GREEN);
        raceColors.put("elf", ChatColor.AQUA);
        raceColors.put("orc", ChatColor.RED);
        raceColors.put("vampire", ChatColor.DARK_PURPLE);
        raceColors.put("dwarf", ChatColor.GOLD);
        // Add more races and colors as needed
    }

    public void initialize() {
        // Create or get the scoreboard
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // Create teams for all existing races
        for (String race : raceColors.keySet()) {
            createTeamForRace(race);
        }

        // Setup nametags for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerNametag(player);
        }
    }

    private void createTeamForRace(String race) {
        String teamName = "race_" + race;

        // Limit team name to 16 characters (Minecraft limitation)
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        // Check if team already exists
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        // Set team prefix (race name and color)
        ChatColor color = raceColors.getOrDefault(race, ChatColor.WHITE);
        team.setPrefix(color + "[" + capitalize(race) + "] " + ChatColor.WHITE);

        // Allow team members to see each other in spectator mode
        team.setCanSeeFriendlyInvisibles(true);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
    }

    public void updatePlayerNametag(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(playerId);

        if (playerData == null) return;

        String race = playerData.getRace();
        if (race == null || race.isEmpty()) {
            race = "human"; // Default race
        }

        String teamName = "race_" + race;
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        // Get or create the team
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            createTeamForRace(race);
            team = scoreboard.getTeam(teamName);
        }

        // Add the player to the team
        team.addEntry(player.getName());

        // Update player display name with rank if LuckPerms is available
        updatePlayerDisplayName(player, race);
    }

    private void updatePlayerDisplayName(Player player, String race) {
        String displayName = player.getName();

        // Add rank prefix if LuckPerms is available
        if (useLuckPerms) {
            try {
                LuckPerms luckPerms = LuckPermsProvider.get();
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());

                if (user != null) {
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    if (prefix != null && !prefix.isEmpty()) {
                        displayName = ChatColor.translateAlternateColorCodes('&', prefix) + " " + displayName;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error getting LuckPerms data for " + player.getName() + ": " + e.getMessage());
            }
        }

        // Add race color
        ChatColor color = raceColors.getOrDefault(race, ChatColor.WHITE);
        displayName = color + displayName;

        // Apply PlaceholderAPI if available
        if (usePlaceholderAPI) {
            displayName = PlaceholderAPI.setPlaceholders(player, displayName);
        }

        player.setDisplayName(displayName + ChatColor.RESET);

        // Set player list name (limited to 16 chars)
        String listName = color + player.getName();
        if (listName.length() > 16) {
            listName = listName.substring(0, 16);
        }
        player.setPlayerListName(listName);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Create player data if it doesn't exist
        if (!plugin.getPlayerManager().hasPlayerData(player.getUniqueId())) {
            plugin.getPlayerManager().createDefaultPlayerData(player.getUniqueId());
        }

        // Update nametag
        updatePlayerNametag(player);
    }

    @EventHandler
    public void onRaceChange(PlayerRaceChangeEvent event) {
        // Update player nametag when race changes
        updatePlayerNametag(event.getPlayer());
    }
}