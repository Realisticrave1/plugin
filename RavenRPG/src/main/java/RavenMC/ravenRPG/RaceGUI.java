package RavenMC.ravenRPG;

import RavenMC.ravenRPG.Managers.PlayerData;
import RavenMC.ravenRPG.Managers.Race;
import RavenMC.ravenRPG.Managers.RaceAbility;
import RavenMC.ravenRPG.RavenRPG;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.InventoryView;

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

        // Add race selection items
        int slot = 10;
        for (Race race : races.values()) {
            ItemStack raceItem = createRaceItem(race);
            inv.setItem(slot, raceItem);
            slot += 2;
        }

        player.openInventory(inv);
    }

    public void openRaceAbilitiesMenu(Player player, Race race) {
        Inventory inv = Bukkit.createInventory(null, 36, ChatColor.DARK_PURPLE + race.getName() + " Abilities");

        List<RaceAbility> abilities = race.getAbilities();
        for (int i = 0; i < abilities.size(); i++) {
            RaceAbility ability = abilities.get(i);
            ItemStack abilityItem = createAbilityItem(ability);
            inv.setItem(10 + i * 2, abilityItem);
        }

        player.openInventory(inv);
    }

    private ItemStack createRaceItem(Race race) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + race.getName());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.WHITE + race.getDescription());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Base Mana: " + race.getBaseMana());
        lore.add(ChatColor.YELLOW + "Mana Regen: " + race.getManaRegen());

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAbilityItem(RaceAbility ability) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GREEN + ability.getName());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.WHITE + ability.getDescription());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Mana Cost: " + ability.getManaCost());
        lore.add(ChatColor.RED + "Cooldown: " + ability.getCooldown() + " seconds");

        meta.setLore(lore);
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

            switch (event.getRawSlot()) {
                case 20: // Race Selection
                    openRaceSelectionMenu(player);
                    break;
                case 22: // Skills
                    // Open skills menu
                    break;
                case 24: // Raven
                    // Open raven menu
                    break;
                case 31: // Economy
                    // Show economy info
                    break;
            }
        } else if (title.equals(ChatColor.DARK_PURPLE + "Race Selection")) {
            event.setCancelled(true);

            // Get clicked race
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                String raceName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                Race selectedRace = races.values().stream()
                        .filter(race -> race.getName().equals(raceName))
                        .findFirst()
                        .orElse(null);

                if (selectedRace != null) {
                    openRaceAbilitiesMenu(player, selectedRace);
                }
            }
        } else if (title.contains("Abilities")) {
            event.setCancelled(true);

            // Handle ability selection
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ENCHANTED_BOOK) {
                String abilityName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

                // Get current player's race
                PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
                String raceName = playerData.getRace();

                Race currentRace = plugin.getRaceManager().getRaces().get(raceName.toLowerCase());

                if (currentRace != null) {
                    // Find the ability by its display name
                    RaceAbility selectedAbility = currentRace.getAbilities().stream()
                            .filter(ability -> ability.getName().equals(abilityName))
                            .findFirst()
                            .orElse(null);

                    if (selectedAbility != null) {
                        // Activate the ability using its ID
                        plugin.getRaceManager().activateRaceAbility(player, selectedAbility.getId());
                        player.closeInventory(); // Close the inventory after ability use
                    }
                }
            }
        }
    }
}