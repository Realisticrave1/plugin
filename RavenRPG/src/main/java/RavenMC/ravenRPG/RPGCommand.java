package RavenMC.ravenRPG;

import RavenMC.ravenRPG.RavenRPG;
import RavenMC.ravenRPG.Managers.PlayerData;
import RavenMC.ravenRPG.Managers.Skill;
import RavenMC.ravenRPG.Managers.Race;
import RavenMC.ravenRPG.Managers.RaceAbility;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class RPGCommand implements CommandExecutor, TabCompleter {

    private final RavenRPG plugin;

    public RPGCommand(RavenRPG plugin) {
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
            return showRPGInfo(player);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info":
                return showRPGInfo(player);

            case "status":
                return showStatus(player);

            case "pay":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /rpg pay <player> <amount>");
                    return false;
                }
                return payPlayer(player, args[1], args[2]);

            case "balance":
                return showBalance(player);

            case "admin":
                if (!player.hasPermission("ravenrpg.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use admin commands.");
                    return true;
                }
                return handleAdminCommand(player, args);

            case "help":
            default:
                return showHelp(player);
        }
    }

    private boolean showRPGInfo(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== RavenRPG System ===");
        player.sendMessage(ChatColor.WHITE + "Welcome to the RavenRPG system! This plugin combines custom ravens, races, skills, and economy.");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Available Commands:");
        player.sendMessage(ChatColor.GREEN + "/raven" + ChatColor.WHITE + " - Manage your personal raven companion");
        player.sendMessage(ChatColor.GREEN + "/race" + ChatColor.WHITE + " - Select and use racial abilities");
        player.sendMessage(ChatColor.GREEN + "/skill" + ChatColor.WHITE + " - View your skills and progression");
        player.sendMessage(ChatColor.GREEN + "/rpg" + ChatColor.WHITE + " - General RPG commands and status");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GREEN + "/rpg help" +
                ChatColor.YELLOW + " for more information.");

        return true;
    }

    private boolean showStatus(Player player) {
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());

        // Get race info
        String raceName = playerData.getRace();
        String raceDisplayName = "Unknown";

        Map<String, Race> races = plugin.getRaceManager().getRaces();
        if (races.containsKey(raceName)) {
            raceDisplayName = races.get(raceName).getName();
        }

        // Get raven info
        String ravenType = "None";
        int ravenLevel = 0;
        if (playerData.getRaven() != null) {
            ravenType = playerData.getRaven().getType();

            // Get raven type name from raven manager
            ravenType = plugin.getRavenManager().formatRavenType(ravenType);
            ravenLevel = playerData.getRaven().getLevel();
        }

        // Show player status
        player.sendMessage(ChatColor.GOLD + "=== " + player.getName() + "'s Status ===");
        player.sendMessage(ChatColor.YELLOW + "Race: " + ChatColor.WHITE + raceDisplayName);
        player.sendMessage(ChatColor.YELLOW + "Mana: " + ChatColor.AQUA +
                playerData.getMana() + "/" + playerData.getMaxMana());
        player.sendMessage(ChatColor.YELLOW + "Balance: " + ChatColor.GREEN +
                plugin.getEconomyProvider().format(playerData.getBalance()));
        player.sendMessage(ChatColor.YELLOW + "Raven: " + ChatColor.WHITE +
                (ravenLevel > 0 ? ravenType + " (Level " + ravenLevel + ")" : "None"));

        // Show skill summary
        player.sendMessage(ChatColor.YELLOW + "Skills:");
        Map<String, Skill> skills = plugin.getSkillManager().getSkills();

        for (Map.Entry<String, Skill> entry : skills.entrySet()) {
            String skillName = entry.getKey();
            Skill skill = entry.getValue();

            String displayName = skill.getName();
            int level = playerData.getSkillLevel(skillName);

            player.sendMessage(ChatColor.GREEN + " - " + displayName + ": " + ChatColor.WHITE + "Level " + level);
        }

        return true;
    }

    private boolean payPlayer(Player player, String targetName, String amountStr) {
        // Find target player
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found: " + targetName);
            return false;
        }

        // Parse amount
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount. Please enter a number.");
            return false;
        }

        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Amount must be positive.");
            return false;
        }

        // Get the economy provider
        net.milkbowl.vault.economy.Economy economy = plugin.getEconomyProvider();

        // Check if player has enough funds
        if (!economy.has(player, amount)) {
            player.sendMessage(ChatColor.RED + "You don't have enough funds. You need " +
                    economy.format(amount) + " but only have " +
                    economy.format(economy.getBalance(player)));
            return false;
        }

        // Transfer the funds
        economy.withdrawPlayer(player, amount);
        economy.depositPlayer(target, amount);

        player.sendMessage(ChatColor.GREEN + "You sent " + economy.format(amount) + " to " + target.getName());
        target.sendMessage(ChatColor.GREEN + "You received " + economy.format(amount) + " from " + player.getName());

        return true;
    }

    private boolean showBalance(Player player) {
        double balance = plugin.getEconomyProvider().getBalance(player);
        player.sendMessage(ChatColor.YELLOW + "Your balance: " +
                ChatColor.GREEN + plugin.getEconomyProvider().format(balance));
        return true;
    }

    private boolean handleAdminCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /rpg admin <set|give|take|reset> ...");
            return false;
        }

        String adminCommand = args[1].toLowerCase();

        switch (adminCommand) {
            case "set":
                if (args.length < 5) {
                    player.sendMessage(ChatColor.RED + "Usage: /rpg admin set <player> <type> <value>");
                    return false;
                }

                String targetName = args[2];
                String type = args[3].toLowerCase();
                String value = args[4];

                return setPlayerData(player, targetName, type, value, args);

            case "give":
                if (args.length < 5) {
                    player.sendMessage(ChatColor.RED + "Usage: /rpg admin give <player> <type> <amount>");
                    return false;
                }

                targetName = args[2];
                type = args[3].toLowerCase();
                String amountStr = args[4];

                return givePlayerData(player, targetName, type, amountStr, args);

            case "reset":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /rpg admin reset <player>");
                    return false;
                }

                targetName = args[2];

                return resetPlayerData(player, targetName);

            default:
                player.sendMessage(ChatColor.RED + "Unknown admin command: " + adminCommand);
                return false;
        }
    }

    private boolean setPlayerData(Player admin, String targetName, String type, String value, String[] args) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            admin.sendMessage(ChatColor.RED + "Player not found: " + targetName);
            return false;
        }

        PlayerData playerData = plugin.getPlayerManager().getPlayerData(target.getUniqueId());

        switch (type) {
            case "race":
                Map<String, Race> races = plugin.getRaceManager().getRaces();
                if (!races.containsKey(value.toLowerCase())) {
                    admin.sendMessage(ChatColor.RED + "Invalid race: " + value);
                    return false;
                }

                plugin.getRaceManager().setPlayerRace(target, value.toLowerCase());
                admin.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s race to " + value);
                return true;

            case "balance":
                double amount;
                try {
                    amount = Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    admin.sendMessage(ChatColor.RED + "Invalid amount: " + value);
                    return false;
                }

                playerData.setBalance(amount);
                plugin.getPlayerManager().savePlayerData(target.getUniqueId());

                admin.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s balance to " +
                        plugin.getEconomyProvider().format(amount));
                return true;

            case "mana":
                int mana;
                try {
                    mana = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    admin.sendMessage(ChatColor.RED + "Invalid mana value: " + value);
                    return false;
                }

                playerData.setMana(mana);
                plugin.getPlayerManager().savePlayerData(target.getUniqueId());

                admin.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s mana to " + mana);
                return true;

            case "maxmana":
                int maxMana;
                try {
                    maxMana = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    admin.sendMessage(ChatColor.RED + "Invalid max mana value: " + value);
                    return false;
                }

                playerData.setMaxMana(maxMana);
                plugin.getPlayerManager().savePlayerData(target.getUniqueId());

                admin.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s max mana to " + maxMana);
                return true;

            case "skilllevel":
                if (args.length < 6) {
                    admin.sendMessage(ChatColor.RED + "Usage: /rpg admin set <player> skilllevel <skill> <level>");
                    return false;
                }

                String skillName = args[4].toLowerCase();
                int level;
                try {
                    level = Integer.parseInt(args[5]);
                } catch (NumberFormatException e) {
                    admin.sendMessage(ChatColor.RED + "Invalid level: " + args[5]);
                    return false;
                }

                Map<String, Skill> skills = plugin.getSkillManager().getSkills();
                if (!skills.containsKey(skillName)) {
                    admin.sendMessage(ChatColor.RED + "Invalid skill: " + skillName);
                    return false;
                }

                playerData.setSkillLevel(skillName, level);
                plugin.getPlayerManager().savePlayerData(target.getUniqueId());

                admin.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s " +
                        skillName + " skill level to " + level);
                return true;

            default:
                admin.sendMessage(ChatColor.RED + "Unknown data type: " + type);
                return false;
        }
    }

    private boolean givePlayerData(Player admin, String targetName, String type, String amountStr, String[] args) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            admin.sendMessage(ChatColor.RED + "Player not found: " + targetName);
            return false;
        }

        PlayerData playerData = plugin.getPlayerManager().getPlayerData(target.getUniqueId());

        switch (type) {
            case "money":
            case "balance":
                double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    admin.sendMessage(ChatColor.RED + "Invalid amount: " + amountStr);
                    return false;
                }

                plugin.getEconomyProvider().depositPlayer(target, amount);

                admin.sendMessage(ChatColor.GREEN + "Gave " + plugin.getEconomyProvider().format(amount) +
                        " to " + target.getName());
                target.sendMessage(ChatColor.GREEN + "You received " +
                        plugin.getEconomyProvider().format(amount) + " from an admin");
                return true;

            case "mana":
                int mana;
                try {
                    mana = Integer.parseInt(amountStr);
                } catch (NumberFormatException e) {
                    admin.sendMessage(ChatColor.RED + "Invalid mana amount: " + amountStr);
                    return false;
                }

                int newMana = Math.min(playerData.getMana() + mana, playerData.getMaxMana());
                playerData.setMana(newMana);
                plugin.getPlayerManager().savePlayerData(target.getUniqueId());

                admin.sendMessage(ChatColor.GREEN + "Gave " + mana + " mana to " + target.getName());
                target.sendMessage(ChatColor.GREEN + "You received " + mana + " mana from an admin");
                return true;

            case "skillxp":
                if (args.length < 6) {
                    admin.sendMessage(ChatColor.RED + "Usage: /rpg admin give <player> skillxp <skill> <amount>");
                    return false;
                }

                String skillName = args[4].toLowerCase();
                int xp;
                try {
                    xp = Integer.parseInt(args[5]);
                } catch (NumberFormatException e) {
                    admin.sendMessage(ChatColor.RED + "Invalid XP amount: " + args[5]);
                    return false;
                }

                Map<String, Skill> skills = plugin.getSkillManager().getSkills();
                if (!skills.containsKey(skillName)) {
                    admin.sendMessage(ChatColor.RED + "Invalid skill: " + skillName);
                    return false;
                }

                plugin.getSkillManager().awardSkillXP(target, skillName, xp);

                admin.sendMessage(ChatColor.GREEN + "Gave " + xp + " XP to " + target.getName() +
                        "'s " + skillName + " skill");
                return true;

            case "ravenxp":
                int ravenXP;
                try {
                    ravenXP = Integer.parseInt(amountStr);
                } catch (NumberFormatException e) {
                    admin.sendMessage(ChatColor.RED + "Invalid XP amount: " + amountStr);
                    return false;
                }

                plugin.getRavenManager().addRavenXp(target.getUniqueId(), ravenXP);

                admin.sendMessage(ChatColor.GREEN + "Gave " + ravenXP + " XP to " +
                        target.getName() + "'s raven");
                target.sendMessage(ChatColor.GREEN + "Your raven gained " + ravenXP + " XP!");
                return true;

            default:
                admin.sendMessage(ChatColor.RED + "Unknown data type: " + type);
                return false;
        }
    }

    private boolean resetPlayerData(Player admin, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            admin.sendMessage(ChatColor.RED + "Player not found: " + targetName);
            return false;
        }

        // Create default player data
        plugin.getPlayerManager().createDefaultPlayerData(target.getUniqueId());

        // Remove raven if exists
        plugin.getRavenManager().destroyRaven(target.getUniqueId());

        admin.sendMessage(ChatColor.GREEN + "Reset all data for " + target.getName());
        target.sendMessage(ChatColor.YELLOW + "Your player data has been reset by an admin.");

        return true;
    }

    private boolean showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== RPG Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/rpg info" + ChatColor.WHITE + " - Show info about the RPG system");
        player.sendMessage(ChatColor.YELLOW + "/rpg status" + ChatColor.WHITE + " - Show your character status");
        player.sendMessage(ChatColor.YELLOW + "/rpg balance" + ChatColor.WHITE + " - Show your current balance");
        player.sendMessage(ChatColor.YELLOW + "/rpg pay <player> <amount>" + ChatColor.WHITE + " - Pay another player");

        if (player.hasPermission("ravenrpg.admin")) {
            player.sendMessage(ChatColor.GOLD + "=== Admin Commands ===");
            player.sendMessage(ChatColor.YELLOW + "/rpg admin set <player> <type> <value>" +
                    ChatColor.WHITE + " - Set player data");
            player.sendMessage(ChatColor.YELLOW + "/rpg admin give <player> <type> <amount>" +
                    ChatColor.WHITE + " - Give resources to a player");
            player.sendMessage(ChatColor.YELLOW + "/rpg admin reset <player>" +
                    ChatColor.WHITE + " - Reset player data");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("info", "status", "balance", "pay", "help"));

            if (sender.hasPermission("ravenrpg.admin")) {
                options.add("admin");
            }

            return options.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("pay")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.startsWith(args[1]))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("ravenrpg.admin")) {
                return Arrays.asList("set", "give", "reset")
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("ravenrpg.admin")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.startsWith(args[2]))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("ravenrpg.admin")) {
                if (args[1].equalsIgnoreCase("set")) {
                    return Arrays.asList("race", "balance", "mana", "maxmana", "skilllevel")
                            .stream()
                            .filter(s -> s.startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                } else if (args[1].equalsIgnoreCase("give")) {
                    return Arrays.asList("money", "mana", "skillxp", "ravenxp")
                            .stream()
                            .filter(s -> s.startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        } else if (args.length == 5) {
            if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("ravenrpg.admin")) {
                if (args[1].equalsIgnoreCase("set") && args[3].equalsIgnoreCase("race")) {
                    return new ArrayList<>(plugin.getRaceManager().getRaces().keySet())
                            .stream()
                            .filter(s -> s.startsWith(args[4].toLowerCase()))
                            .collect(Collectors.toList());
                } else if ((args[1].equalsIgnoreCase("set") && args[3].equalsIgnoreCase("skilllevel")) ||
                        (args[1].equalsIgnoreCase("give") && args[3].equalsIgnoreCase("skillxp"))) {
                    return new ArrayList<>(plugin.getSkillManager().getSkills().keySet())
                            .stream()
                            .filter(s -> s.startsWith(args[4].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }

        return new ArrayList<>();
    }
}