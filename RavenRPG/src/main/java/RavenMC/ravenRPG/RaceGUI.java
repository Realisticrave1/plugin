package RavenMC.ravenRPG;

import RavenMC.ravenRPG.Managers.PlayerData;
import RavenMC.ravenRPG.Managers.Race;
import RavenMC.ravenRPG.Managers.RaceAbility;
import RavenMC.ravenRPG.RavenRPG;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RaceGUI implements Listener {
    private final RavenRPG plugin;
    private final Map<String, Race> races;

    public RaceGUI(RavenRPG plugin) {
        this.plugin = plugin;
        this.races = plugin.getRaceManager().getRaces();
    }

    public void openMainRPGMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "RavenRPG Main Menu");

        // Decorative glass panes for border
        ItemStack border = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(i + 45, border);
        }
        for (int i = 0; i < 5; i++) {
            inv.setItem(i * 9, border);
            inv.setItem(i * 9 + 8, border);
        }

        // Race Selection Item
        ItemStack raceItem = createGuiItem(Material.PLAYER_HEAD,
                ChatColor.GOLD + "Race Selection",
                ChatColor.YELLOW + "Choose your race and view abilities");
        inv.setItem(20, raceItem);

        // Skills Item
        ItemStack skillsItem = createGuiItem(Material.BOOK,
                ChatColor.GREEN + "Skills",
                ChatColor.YELLOW + "View and upgrade your skills");
        inv.setItem(22, skillsItem);

        // Raven Item
        ItemStack ravenItem = createGuiItem(Material.FEATHER,
                ChatColor.AQUA + "Raven Companion",
                ChatColor.YELLOW + "Manage your magical raven");
        inv.setItem(24, ravenItem);

        // Economy Item
        ItemStack economyItem = createGuiItem(Material.GOLD_INGOT,
                ChatColor.YELLOW + "Economy",
                ChatColor.YELLOW + "Check your balance and transactions");
        inv.setItem(31, economyItem);

        player.openInventory(inv);
    }

    public void openRaceSelectionMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "Race Selection");

        // Decorative glass panes for border
        ItemStack border = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(i + 45, border);
        }
        for (int i = 0; i < 5; i++) {
            inv.setItem(i * 9, border);
            inv.setItem(i * 9 + 8, border);
        }

        // Add race selection items with proper icons
        int[] slots = {11, 13, 15, 29, 31, 33};
        int index = 0;
        for (Race race : races.values()) {
            if (index >= slots.length) break;

            ItemStack raceItem = createEnhancedRaceItem(race);
            inv.setItem(slots[index], raceItem);
            index++;
        }

        // Add information item
        ItemStack infoItem = createGuiItem(Material.BOOK,
                ChatColor.GOLD + "Race Information",
                ChatColor.YELLOW + "Left-click on a race to view its abilities",
                ChatColor.YELLOW + "Right-click to select a race");
        inv.setItem(4, infoItem);

        // Current race indicator
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        String currentRaceName = playerData.getRace();
        Race currentRace = races.get(currentRaceName.toLowerCase());
        if (currentRace != null) {
            ItemStack currentRaceItem = createGuiItem(Material.NETHER_STAR,
                    ChatColor.GREEN + "Current Race: " + currentRace.getName(),
                    ChatColor.WHITE + currentRace.getDescription(),
                    "",
                    ChatColor.YELLOW + "Mana: " + ChatColor.AQUA + playerData.getMana() + "/" + playerData.getMaxMana());
            inv.setItem(49, currentRaceItem);
        }

        player.openInventory(inv);
    }

    private ItemStack createEnhancedRaceItem(Race race) {
        // Choose appropriate material based on race type
        Material material;
        switch(race.getId().toLowerCase()) {
            case "human":
                material = Material.PLAYER_HEAD;
                break;
            case "elf":
                material = Material.BOW;
                break;
            case "orc":
                material = Material.IRON_AXE;
                break;
            case "vampire":
                material = Material.REDSTONE;
                break;
            case "dwarf":
                material = Material.IRON_PICKAXE;
                break;
            default:
                material = Material.PLAYER_HEAD;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + race.getName());
        List<String> lore = new ArrayList<>();

        // Split description into multiple lines if needed
        String description = race.getDescription();
        List<String> descLines = new ArrayList<>();

        // Manual word wrapping
        StringBuilder currentLine = new StringBuilder();
        String[] words = description.split(" ");
        for (String word : words) {
            if (currentLine.length() + word.length() > 30) {
                descLines.add(currentLine.toString());
                currentLine = new StringBuilder();
            }
            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }
        if (currentLine.length() > 0) {
            descLines.add(currentLine.toString());
        }

        for (String line : descLines) {
            lore.add(ChatColor.WHITE + line);
        }

        lore.add("");
        lore.add(ChatColor.YELLOW + "Base Mana: " + ChatColor.AQUA + race.getBaseMana());
        lore.add(ChatColor.YELLOW + "Mana Regen: " + ChatColor.AQUA + race.getManaRegen() + " per tick");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Abilities:");

        for (RaceAbility ability : race.getAbilities()) {
            lore.add(ChatColor.GREEN + " • " + ability.getName());
        }

        lore.add("");
        lore.add(ChatColor.YELLOW + "Left-Click: " + ChatColor.WHITE + "View Abilities");
        lore.add(ChatColor.YELLOW + "Right-Click: " + ChatColor.WHITE + "Select Race");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void openRaceAbilitiesMenu(Player player, Race race) {
        Inventory inv = Bukkit.createInventory(null, 36, ChatColor.DARK_PURPLE + race.getName() + " Abilities");

        // Border
        ItemStack border = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(i + 27, border);
        }
        for (int i = 1; i < 3; i++) {
            inv.setItem(i * 9, border);
            inv.setItem(i * 9 + 8, border);
        }

        // Race info item
        ItemStack raceItem = createEnhancedRaceItem(race);
        inv.setItem(4, raceItem);

        // Abilities with cooldown display
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        List<RaceAbility> abilities = race.getAbilities();
        int[] slots = {11, 13, 15, 22};

        // Check if this is the player's current race
        boolean isCurrentRace = playerData.getRace().equalsIgnoreCase(race.getId());

        for (int i = 0; i < abilities.size() && i < slots.length; i++) {
            RaceAbility ability = abilities.get(i);

            // Only show cooldowns for the player's current race
            int cooldown = 0;
            if (isCurrentRace) {
                cooldown = plugin.getRaceManager().getRemainingCooldown(player.getUniqueId(), ability.getId());
            }

            ItemStack abilityItem = createEnhancedAbilityItem(ability, cooldown);
            inv.setItem(slots[i], abilityItem);
        }

        // Back button
        ItemStack backButton = createGuiItem(Material.ARROW, ChatColor.YELLOW + "Back to Race Selection");
        inv.setItem(31, backButton);

        player.openInventory(inv);
    }

    private ItemStack createEnhancedAbilityItem(RaceAbility ability, int cooldown) {
        // Choose appropriate material based on ability type
        Material material;
        if (ability.getId().contains("speed") || ability.getId().contains("swift")) {
            material = Material.FEATHER;
        } else if (ability.getId().contains("resist") || ability.getId().contains("skin") || ability.getId().contains("shield")) {
            material = Material.IRON_CHESTPLATE;
        } else if (ability.getId().contains("vision") || ability.getId().contains("sight")) {
            material = Material.ENDER_EYE;
        } else if (ability.getId().contains("heal") || ability.getId().contains("bond") || ability.getId().contains("regen")) {
            material = Material.GOLDEN_APPLE;
        } else if (ability.getId().contains("blood") || ability.getId().contains("drain")) {
            material = Material.REDSTONE;
        } else if (ability.getId().contains("damage") || ability.getId().contains("battle") || ability.getId().contains("cry")) {
            material = Material.DIAMOND_SWORD;
        } else if (ability.getId().contains("hammer_time") || ability.getId().contains("hammer")) {
            material = Material.IRON_AXE;
        } else {
            material = Material.ENCHANTED_BOOK;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GREEN + ability.getName());
        List<String> lore = new ArrayList<>();

        // Split description into multiple lines if needed
        String description = ability.getDescription();
        List<String> descLines = new ArrayList<>();

        // Manual word wrapping
        StringBuilder currentLine = new StringBuilder();
        String[] words = description.split(" ");
        for (String word : words) {
            if (currentLine.length() + word.length() > 30) {
                descLines.add(currentLine.toString());
                currentLine = new StringBuilder();
            }
            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }
        if (currentLine.length() > 0) {
            descLines.add(currentLine.toString());
        }

        for (String line : descLines) {
            lore.add(ChatColor.WHITE + line);
        }

        lore.add("");
        lore.add(ChatColor.YELLOW + "Mana Cost: " + ChatColor.AQUA + ability.getManaCost());

        // Display cooldown info
        if (cooldown > 0) {
            lore.add(ChatColor.RED + "Cooldown: " + cooldown + " seconds remaining");
        } else {
            lore.add(ChatColor.YELLOW + "Cooldown: " + ChatColor.WHITE + ability.getCooldown() + " seconds");
        }

        lore.add("");
        lore.add(ChatColor.YELLOW + "Click: " + ChatColor.WHITE + "Use Ability");

        meta.setLore(lore);

        // Add glowing effect for ready abilities
        if (cooldown <= 0) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        if (lore != null) {
            meta.setLore(Arrays.asList(lore));
        }

        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals(ChatColor.DARK_PURPLE + "RavenRPG Main Menu")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            switch (event.getRawSlot()) {
                case 20: // Race Selection
                    openRaceSelectionMenu(player);
                    break;
                case 22: // Skills
                    // Open skills menu
                    player.performCommand("skill list");
                    player.closeInventory();
                    break;
                case 24: // Raven
                    // Open raven menu
                    player.performCommand("raven info");
                    player.closeInventory();
                    break;
                case 31: // Economy
                    // Show economy info
                    player.performCommand("rpg balance");
                    player.closeInventory();
                    break;
            }
        } else if (title.equals(ChatColor.DARK_PURPLE + "Race Selection")) {
            event.setCancelled(true);

            // Get clicked item
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR
                    || event.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE) {
                return;
            }

            // Check for click on race item (not border or info items)
            if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
                String raceName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                Race selectedRace = null;

                for (Race race : races.values()) {
                    if (race.getName().equals(raceName)) {
                        selectedRace = race;
                        break;
                    }
                }

                if (selectedRace != null) {
                    // Left click to view abilities, right click to select
                    if (event.isLeftClick()) {
                        openRaceAbilitiesMenu(player, selectedRace);
                        // Play sound effect
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                    } else if (event.isRightClick()) {
                        // Select the race
                        plugin.getRaceManager().setPlayerRace(player, selectedRace.getId());
                        // Play sound effect
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                        // Refresh the inventory to show updated current race
                        openRaceSelectionMenu(player);
                    }
                }
            }
        } else if (title.contains("Abilities")) {
            event.setCancelled(true);

            // Get clicked item
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR
                    || event.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE) {
                return;
            }

            // Back button
            if (event.getRawSlot() == 31 && event.getCurrentItem().getType() == Material.ARROW) {
                openRaceSelectionMenu(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                return;
            }

            // Handle ability click
            if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
                String abilityName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

                // Get current player's race
                PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
                String raceName = playerData.getRace();
                Race currentRace = plugin.getRaceManager().getRaces().get(raceName.toLowerCase());

                if (currentRace != null) {
                    // Find the ability by its display name
                    for (RaceAbility ability : currentRace.getAbilities()) {
                        if (ability.getName().equals(abilityName)) {
                            // Check if the ability is on cooldown
                            if (plugin.getRaceManager().isOnCooldown(player.getUniqueId(), ability.getId())) {
                                int remainingTime = plugin.getRaceManager().getRemainingCooldown(player.getUniqueId(), ability.getId());
                                player.sendMessage(ChatColor.RED + "This ability is on cooldown for " +
                                        remainingTime + " more seconds!");
                                // Play error sound
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                                return;
                            }

                            // Activate the ability
                            plugin.getRaceManager().activateRaceAbility(player, ability.getId());
                            player.closeInventory();

                            // Re-open after a delay to show updated cooldowns
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                openRaceAbilitiesMenu(player, currentRace);
                            }, 5L);

                            return;
                        }
                    }
                }
            }
        }
    }
}