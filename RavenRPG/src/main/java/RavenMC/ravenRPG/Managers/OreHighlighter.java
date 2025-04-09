package RavenMC.ravenRPG.Managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class OreHighlighter {
    private final Plugin plugin;
    private final Map<Player, List<Integer>> activeHighlights = new HashMap<>();
    private final ProtocolManager protocolManager;

    public OreHighlighter(Plugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    /**
     * Shows glowing outline around ore blocks for a player
     * @param player The player to show the glowing blocks to
     * @param ores List of ore blocks to highlight
     * @param duration Duration in seconds before the highlight disappears
     */
    public void highlightOres(Player player, List<Block> ores, int duration) {
        if (ores.isEmpty() || player == null) {
            return;
        }

        // Remove any existing highlights for this player
        removeHighlights(player);

        // Create a list to store entity IDs
        List<Integer> entityIds = new ArrayList<>();
        activeHighlights.put(player, entityIds);

        // Create teams for different colored glows
        setupTeams(player);

        // For each ore block, create a shulker entity with glowing effect
        for (Block ore : ores) {
            // Generate a random entity ID
            int entityId = ThreadLocalRandom.current().nextInt(1000000, 2000000);
            entityIds.add(entityId);

            // Get location centered on block
            Location loc = ore.getLocation().add(0.5, 0, 0.5);

            // Create spawn packet
            PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            spawnPacket.getIntegers().write(0, entityId); // Entity ID
            spawnPacket.getEntityTypeModifier().write(0, EntityType.SHULKER);
            spawnPacket.getDoubles()
                    .write(0, loc.getX())
                    .write(1, loc.getY())
                    .write(2, loc.getZ());

            // Create metadata packet (for glowing and invisibility)
            PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);

            WrappedDataWatcher watcher = new WrappedDataWatcher();
            watcher.setObject(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x60); // 0x60 = invisible + glowing
            watcher.setObject(5, WrappedDataWatcher.Registry.get(Boolean.class), true); // Invisibility flag

            metadataPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

            // Send packets to player
            try {
                protocolManager.sendServerPacket(player, spawnPacket);
                protocolManager.sendServerPacket(player, metadataPacket);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            // Add entity to team for colored glow
            addEntityToTeam(player, entityId, getOreTeamName(ore.getType()));
        }

        // Schedule removal of all entities after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                removeHighlights(player);
            }
        }.runTaskLater(plugin, duration * 20L); // Convert seconds to ticks
    }

    /**
     * Removes all active highlights for a player
     */
    public void removeHighlights(Player player) {
        if (!activeHighlights.containsKey(player) || !player.isOnline()) {
            return;
        }

        List<Integer> entityIds = activeHighlights.get(player);
        if (entityIds.isEmpty()) {
            return;
        }

        // Create destroy packet for all entities
        PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);

        // Add entity IDs to list for packet
        List<Integer> idList = new ArrayList<>(entityIds);
        destroyPacket.getIntLists().write(0, idList);

        // Send destroy packet
        try {
            protocolManager.sendServerPacket(player, destroyPacket);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        // Reset scoreboard
        try {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        } catch (Exception e) {
            plugin.getLogger().warning("Error resetting scoreboard: " + e.getMessage());
        }

        // Remove from active highlights
        activeHighlights.remove(player);
    }

    /**
     * Sets up teams for different ore colors
     */
    private void setupTeams(Player player) {
        // Create a new scoreboard for this player
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        // Create teams for each ore type
        Team diamondTeam = board.registerNewTeam("diamond_ore");
        diamondTeam.setColor(ChatColor.AQUA);

        Team emeraldTeam = board.registerNewTeam("emerald_ore");
        emeraldTeam.setColor(ChatColor.GREEN);

        Team goldTeam = board.registerNewTeam("gold_ore");
        goldTeam.setColor(ChatColor.GOLD);

        Team redstoneTeam = board.registerNewTeam("redstone_ore");
        redstoneTeam.setColor(ChatColor.RED);

        Team lapisTeam = board.registerNewTeam("lapis_ore");
        lapisTeam.setColor(ChatColor.BLUE);

        Team ironTeam = board.registerNewTeam("iron_ore");
        ironTeam.setColor(ChatColor.WHITE);

        Team copperTeam = board.registerNewTeam("copper_ore");
        copperTeam.setColor(ChatColor.GOLD);

        Team debrisTeam = board.registerNewTeam("ancient_debris");
        debrisTeam.setColor(ChatColor.DARK_PURPLE);

        Team coalTeam = board.registerNewTeam("coal_ore");
        coalTeam.setColor(ChatColor.DARK_GRAY);

        Team otherTeam = board.registerNewTeam("other_ore");
        otherTeam.setColor(ChatColor.YELLOW);

        // Set the player's scoreboard
        player.setScoreboard(board);
    }

    /**
     * Adds an entity to a team for colored glow
     */
    private void addEntityToTeam(Player player, int entityId, String teamName) {
        // Get the player's current scoreboard
        Scoreboard board = player.getScoreboard();

        // Get team
        Team team = board.getTeam(teamName);
        if (team == null) {
            return;
        }

        // Create a unique entry for this entity
        String entry = "ore-" + entityId;

        // Add entry to team
        team.addEntry(entry);
    }

    /**
     * Gets the team name for an ore type
     */
    private String getOreTeamName(Material material) {
        String matName = material.name().toLowerCase();

        if (matName.contains("diamond")) {
            return "diamond_ore";
        } else if (matName.contains("emerald")) {
            return "emerald_ore";
        } else if (matName.contains("gold")) {
            return "gold_ore";
        } else if (matName.contains("redstone")) {
            return "redstone_ore";
        } else if (matName.contains("lapis")) {
            return "lapis_ore";
        } else if (matName.contains("iron")) {
            return "iron_ore";
        } else if (matName.contains("copper")) {
            return "copper_ore";
        } else if (matName.contains("ancient_debris")) {
            return "ancient_debris";
        } else if (matName.contains("coal")) {
            return "coal_ore";
        }

        return "other_ore";
    }
}