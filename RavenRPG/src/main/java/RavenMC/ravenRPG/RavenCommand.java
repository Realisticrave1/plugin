package RavenMC.ravenRPG;

import RavenMC.ravenRPG.Managers.RavenManager;
import RavenMC.ravenRPG.RavenRPG;
import RavenMC.ravenRPG.Managers.PlayerData;
import RavenMC.ravenRPG.Managers.PlayerRaven;
import RavenMC.ravenRPG.Managers.RavenType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class RavenCommand implements CommandExecutor, TabCompleter {

    private final RavenRPG plugin;

    public RavenCommand(RavenRPG plugin) {
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
            return showRavenInfo(player);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "summon":
            case "call":
                plugin.getRavenManager().createRavenFor(player);
                return true;

            case "dismiss":
                plugin.getRavenManager().destroyRaven(player.getUniqueId());
                player.sendMessage(ChatColor.GOLD + "Your raven has been dismissed.");
                return true;

            case "info":
                return showRavenInfo(player);

            case "color":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /raven color <color>");
                    return false;
                }
                return changeRavenColor(player, args[1]);

            case "type":
            case "change":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /raven type <type>");
                    return false;
                }
                return changeRavenType(player, args[1]);

            case "ability":
            case "use":
                plugin.getRavenManager().activateRavenAbility(player);
                return true;

            case "list":
                return listRavenTypes(player);

            case "help":
            default:
                return showHelp(player);
        }
    }

    private boolean showRavenInfo(Player player) {
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());

        if (playerData.getRaven() == null) {
            player.sendMessage(ChatColor.RED + "You don't have a raven yet. Use /raven summon to call one.");
            return true;
        }

        PlayerRaven raven = playerData.getRaven();
        String type = raven.getType();
        int level = raven.getLevel();
        int xp = raven.getXp();
        int nextLevelXP = calculateXPForLevel(level + 1) - calculateXPForLevel(level);

        Map<String, RavenManager.RavenType> ravenTypes = plugin.getRavenManager().getRavenTypes();
        RavenManager.RavenType ravenType = ravenTypes.get(type);

        player.sendMessage(ChatColor.GOLD + "=== Your Raven ===");
        player.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE +
                (ravenType != null ? ravenType.getName() : type));
        player.sendMessage(ChatColor.YELLOW + "Level: " + ChatColor.WHITE + level);
        player.sendMessage(ChatColor.YELLOW + "XP: " + ChatColor.WHITE + xp + "/" + nextLevelXP);

        if (ravenType != null) {
            List<String> abilities = ravenType.getAbilities();
            if (abilities != null && !abilities.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "Abilities:");
                for (String ability : abilities) {
                    player.sendMessage(ChatColor.WHITE + " - " + ability);
                }
            }
        }

        player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GREEN + "/raven ability" +
                ChatColor.YELLOW + " to activate your raven's power.");

        return true;
    }

    private int calculateXPForLevel(int level) {
        // Same formula as in RavenManager
        return 100 * (level * level);
    }

    private boolean changeRavenColor(Player player, String colorName) {
        int colorCode;
        try {
            // Try to parse as a hex color
            if (colorName.startsWith("#")) {
                colorCode = Integer.parseInt(colorName.substring(1), 16);
            } else {
                // Try to parse as a named color
                colorCode = getColorCodeFromName(colorName);
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid color format. Use a color name or hex code (#RRGGBB).");
            return false;
        }

        plugin.getRavenManager().changeRavenColor(player, colorCode);
        return true;
    }

    private int getColorCodeFromName(String colorName) {
        // Common color names and their RGB values
        switch (colorName.toLowerCase()) {
            case "black": return 0x000000;
            case "white": return 0xFFFFFF;
            case "red": return 0xFF0000;
            case "green": return 0x00FF00;
            case "blue": return 0x0000FF;
            case "yellow": return 0xFFFF00;
            case "purple": return 0x800080;
            case "orange": return 0xFFA500;
            case "pink": return 0xFFC0CB;
            case "gray": case "grey": return 0x808080;
            case "brown": return 0x8B4513;
            case "cyan": return 0x00FFFF;
            case "lime": return 0x32CD32;
            case "magenta": return 0xFF00FF;
            case "silver": return 0xC0C0C0;
            case "gold": return 0xFFD700;
            default:
                throw new NumberFormatException("Unknown color name: " + colorName);
        }
    }

    private boolean changeRavenType(Player player, String typeName) {
        Map<String, RavenManager.RavenType> ravenTypes = plugin.getRavenManager().getRavenTypes();

        if (!ravenTypes.containsKey(typeName.toLowerCase())) {
            player.sendMessage(ChatColor.RED + "Invalid raven type. Use /raven list to see available types.");
            return false;
        }

        plugin.getRavenManager().changeRavenType(player, typeName.toLowerCase());
        return true;
    }

    private boolean listRavenTypes(Player player) {
        Map<String, RavenManager.RavenType> ravenTypes = plugin.getRavenManager().getRavenTypes();

        player.sendMessage(ChatColor.GOLD + "=== Available Raven Types ===");

        for (Map.Entry<String, RavenManager.RavenType> entry : ravenTypes.entrySet()) {
            RavenManager.RavenType ravenType = entry.getValue();

            player.sendMessage(ChatColor.YELLOW + ravenType.getName() +
                    ChatColor.WHITE + ": " + ravenType.getDescription());
        }

        return true;
    }

    private boolean showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Raven Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/raven summon" + ChatColor.WHITE + " - Call your raven");
        player.sendMessage(ChatColor.YELLOW + "/raven dismiss" + ChatColor.WHITE + " - Dismiss your raven");
        player.sendMessage(ChatColor.YELLOW + "/raven info" + ChatColor.WHITE + " - Show information about your raven");
        player.sendMessage(ChatColor.YELLOW + "/raven ability" + ChatColor.WHITE + " - Use your raven's special ability");
        player.sendMessage(ChatColor.YELLOW + "/raven color <color>" + ChatColor.WHITE + " - Change your raven's color");
        player.sendMessage(ChatColor.YELLOW + "/raven type <type>" + ChatColor.WHITE + " - Change your raven's type");
        player.sendMessage(ChatColor.YELLOW + "/raven list" + ChatColor.WHITE + " - List available raven types");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("summon", "dismiss", "info", "ability", "color", "type", "list", "help")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("color")) {
                return Arrays.asList("black", "white", "red", "green", "blue", "yellow", "purple",
                                "orange", "pink", "gray", "brown", "cyan", "lime", "magenta", "gold")
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("type")) {
                return new ArrayList<>(plugin.getRavenManager().getRavenTypes().keySet())
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}