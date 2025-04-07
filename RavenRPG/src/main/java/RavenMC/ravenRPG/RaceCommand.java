package RavenMC.ravenRPG;

import RavenMC.ravenRPG.RavenRPG;
import RavenMC.ravenRPG.Managers.PlayerData;
import RavenMC.ravenRPG.Managers.Race;
import RavenMC.ravenRPG.Managers.RaceAbility;
import RavenMC.ravenRPG.RaceGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class RaceCommand implements CommandExecutor, TabCompleter {

    private final RavenRPG plugin;
    private final RaceGUI raceGUI;

    public RaceCommand(RavenRPG plugin) {
        this.plugin = plugin;
        this.raceGUI = new RaceGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            return showRaceInfo(player);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info":
                return showRaceInfo(player);

            case "select":
            case "choose":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /race select <race>");
                    return false;
                }
                return selectRace(player, args[1]);

            case "list":
                return listRaces(player);

            case "ability":
            case "power":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /race ability <ability>");
                    return false;
                }
                return useRaceAbility(player, args[1]);

            case "abilities":
                return listRaceAbilities(player);

            case "gui":
                raceGUI.openRaceSelectionMenu(player);
                return true;

            case "help":
            default:
                return showHelp(player);
        }
    }

    private boolean showRaceInfo(Player player) {
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        String raceName = playerData.getRace();

        Map<String, Race> races = plugin.getRaceManager().getRaces();
        Race race = races.get(raceName);

        if (race == null) {
            player.sendMessage(ChatColor.RED + "You don't have a valid race. Use /race select to choose one.");
            return true;
        }

        String displayName = race.getName();
        String description = race.getDescription();

        player.sendMessage(ChatColor.GOLD + "=== Your Race: " + displayName + " ===");
        player.sendMessage(ChatColor.WHITE + description);
        player.sendMessage(ChatColor.YELLOW + "Mana: " + ChatColor.AQUA + playerData.getMana() +
                "/" + playerData.getMaxMana());

        // Show abilities
        player.sendMessage(ChatColor.YELLOW + "Abilities:");
        List<RaceAbility> abilities = race.getAbilities();
        if (abilities != null) {
            for (RaceAbility ability : abilities) {
                String abilityName = ability.getName();
                String abilityDesc = ability.getDescription();
                int manaCost = ability.getManaCost();

                player.sendMessage(ChatColor.GREEN + " - " + abilityName +
                        ChatColor.WHITE + ": " + abilityDesc +
                        ChatColor.AQUA + " (Mana: " + manaCost + ")");
            }
        }

        player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GREEN + "/race ability <name>" +
                ChatColor.YELLOW + " to use an ability.");

        return true;
    }
    private boolean selectRace(Player player, String raceName) {
        Map<String, Race> races = plugin.getRaceManager().getRaces();

        if (!races.containsKey(raceName.toLowerCase())) {
            player.sendMessage(ChatColor.RED + "Invalid race name. Use /race list to see available races.");
            return false;
        }

        plugin.getRaceManager().setPlayerRace(player, raceName.toLowerCase());
        return true;
    }

    private boolean listRaces(Player player) {
        Map<String, Race> races = plugin.getRaceManager().getRaces();

        player.sendMessage(ChatColor.GOLD + "=== Available Races ===");

        for (Map.Entry<String, Race> entry : races.entrySet()) {
            Race race = entry.getValue();
            String displayName = race.getName();
            String description = race.getDescription();

            player.sendMessage(ChatColor.YELLOW + displayName + ChatColor.WHITE + ": " + description);

            // Show race abilities
            List<RaceAbility> abilities = race.getAbilities();
            if (abilities != null && !abilities.isEmpty()) {
                player.sendMessage(ChatColor.GREEN + "  Abilities:");
                for (RaceAbility ability : abilities) {
                    player.sendMessage(ChatColor.GREEN + "   - " + ability.getName() +
                            ChatColor.WHITE + ": " + ability.getDescription());
                }
            }
        }

        return true;
    }

    private boolean useRaceAbility(Player player, String abilityName) {
        // First, try to use the ability by name
        plugin.getRaceManager().activateRaceAbility(player, abilityName.toLowerCase());
        return true;
    }

    private boolean listRaceAbilities(Player player) {
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        String raceName = playerData.getRace();

        Map<String, Race> races = plugin.getRaceManager().getRaces();
        Race race = races.get(raceName);

        if (race == null) {
            player.sendMessage(ChatColor.RED + "You don't have a valid race. Use /race select to choose one.");
            return true;
        }

        String displayName = race.getName();

        player.sendMessage(ChatColor.GOLD + "=== " + displayName + " Abilities ===");

        List<RaceAbility> abilities = race.getAbilities();
        if (abilities != null && !abilities.isEmpty()) {
            for (RaceAbility ability : abilities) {
                String abilityId = ability.getId();
                String abilityName = ability.getName();
                String abilityDesc = ability.getDescription();
                int manaCost = ability.getManaCost();
                int cooldown = ability.getCooldown();

                player.sendMessage(ChatColor.YELLOW + abilityName + ChatColor.WHITE +
                        " (" + abilityId + "): " + abilityDesc);
                player.sendMessage(ChatColor.AQUA + "Mana Cost: " + manaCost +
                        ChatColor.RED + " Cooldown: " + (cooldown / 60) + "m " + (cooldown % 60) + "s");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Your race has no special abilities.");
        }

        return true;
    }

    private boolean showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Race Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/race info" + ChatColor.WHITE + " - Show information about your race");
        player.sendMessage(ChatColor.YELLOW + "/race select <race>" + ChatColor.WHITE + " - Choose a race");
        player.sendMessage(ChatColor.YELLOW + "/race list" + ChatColor.WHITE + " - List available races");
        player.sendMessage(ChatColor.YELLOW + "/race ability <ability>" + ChatColor.WHITE + " - Use a race ability");
        player.sendMessage(ChatColor.YELLOW + "/race abilities" + ChatColor.WHITE + " - List your race's abilities");
        player.sendMessage(ChatColor.YELLOW + "/race gui" + ChatColor.WHITE + " - Open race selection GUI");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("info", "select", "list", "ability", "abilities", "gui", "help")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("select") || args[0].equalsIgnoreCase("choose")) {
                return new ArrayList<>(plugin.getRaceManager().getRaces().keySet())
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("ability") || args[0].equalsIgnoreCase("power")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
                    String raceName = playerData.getRace();

                    Race race = plugin.getRaceManager().getRaces().get(raceName);

                    if (race != null) {
                        return race.getAbilities().stream()
                                .map(RaceAbility::getId)
                                .filter(s -> s.startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                }
            }
        }

        return new ArrayList<>();
    }
}