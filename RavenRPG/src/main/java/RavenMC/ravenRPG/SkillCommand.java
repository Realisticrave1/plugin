package RavenMC.ravenRPG;

import RavenMC.ravenRPG.RavenRPG;
import RavenMC.ravenRPG.Managers.PlayerData;
import RavenMC.ravenRPG.Managers.Skill;
import RavenMC.ravenRPG.Managers.SkillReward;  // Make sure this import is correct
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class SkillCommand implements CommandExecutor, TabCompleter {

    private final RavenRPG plugin;

    public SkillCommand(RavenRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            return listSkills(player);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /skill info <skill>");
                    return false;
                }
                return showSkillInfo(player, args[1]);

            case "list":
                return listSkills(player);

            case "help":
            default:
                return showHelp(player);
        }
    }

    private boolean showSkillInfo(Player player, String skillName) {
        Map<String, Skill> skills = plugin.getSkillManager().getSkills();

        if (!skills.containsKey(skillName.toLowerCase())) {
            player.sendMessage(ChatColor.RED + "Invalid skill name. Use /skill list to see available skills.");
            return false;
        }

        Skill skill = skills.get(skillName.toLowerCase());
        String displayName = skill.getName();
        String description = skill.getDescription();

        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        int level = playerData.getSkillLevel(skillName.toLowerCase());
        int xp = playerData.getSkillXP(skillName.toLowerCase());
        int nextLevelXP = calculateXPForLevel(level + 1) - calculateXPForLevel(level);

        player.sendMessage(ChatColor.GOLD + "=== " + displayName + " Skill ===");
        player.sendMessage(ChatColor.WHITE + description);
        player.sendMessage(ChatColor.YELLOW + "Level: " + ChatColor.WHITE + level);
        player.sendMessage(ChatColor.YELLOW + "XP: " + ChatColor.WHITE + xp + "/" + nextLevelXP);

        // Show rewards
        player.sendMessage(ChatColor.YELLOW + "Unlocked Perks:");
        Map<Integer, SkillReward> rewards = skill.getRewards();
        if (rewards != null) {
            for (Map.Entry<Integer, SkillReward> entry : rewards.entrySet()) {
                int rewardLevel = entry.getKey();
                if (rewardLevel <= level) {
                    SkillReward reward = entry.getValue();
                    String rewardDescription = reward.getDescription();

                    player.sendMessage(ChatColor.GREEN + " - Level " + rewardLevel + ": " +
                            ChatColor.WHITE + rewardDescription);
                }
            }
        }

        // Show upcoming rewards
        player.sendMessage(ChatColor.YELLOW + "Upcoming Perks:");
        if (rewards != null) {
            boolean foundUpcoming = false;
            for (Map.Entry<Integer, SkillReward> entry : rewards.entrySet()) {
                int rewardLevel = entry.getKey();
                if (rewardLevel > level && rewardLevel <= level + 5) { // Show next 5 rewards
                    SkillReward reward = entry.getValue();
                    String rewardDescription = reward.getDescription();

                    player.sendMessage(ChatColor.RED + " - Level " + rewardLevel + ": " +
                            ChatColor.WHITE + rewardDescription);
                    foundUpcoming = true;
                }
            }

            if (!foundUpcoming) {
                player.sendMessage(ChatColor.RED + " None in the next 5 levels.");
            }
        }

        return true;
    }

    private int calculateXPForLevel(int level) {
        // Same formula as in SkillManager
        return 100 * level * level;
    }

    private boolean listSkills(Player player) {
        Map<String, Skill> skills = plugin.getSkillManager().getSkills();
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());

        player.sendMessage(ChatColor.GOLD + "=== Your Skills ===");

        for (Map.Entry<String, Skill> entry : skills.entrySet()) {
            String skillName = entry.getKey();
            Skill skill = entry.getValue();

            String displayName = skill.getName();
            int level = playerData.getSkillLevel(skillName);
            int xp = playerData.getSkillXP(skillName);
            int nextLevelXP = calculateXPForLevel(level + 1) - calculateXPForLevel(level);

            // Calculate percentage to next level
            int percent = (int) ((double) xp / nextLevelXP * 100);

            player.sendMessage(ChatColor.YELLOW + displayName + ": " +
                    ChatColor.GREEN + "Level " + level +
                    ChatColor.WHITE + " (" + percent + "% to next level)");
        }

        player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GREEN + "/skill info <skill>" +
                ChatColor.YELLOW + " for detailed information.");

        return true;
    }

    private boolean showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Skill Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/skill list" + ChatColor.WHITE + " - List all of your skills");
        player.sendMessage(ChatColor.YELLOW + "/skill info <skill>" + ChatColor.WHITE + " - Show detailed skill information");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("info", "list", "help")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info")) {
                return new ArrayList<>(plugin.getSkillManager().getSkills().keySet())
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}