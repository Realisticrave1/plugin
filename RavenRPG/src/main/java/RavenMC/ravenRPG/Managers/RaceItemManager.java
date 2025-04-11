package RavenMC.ravenRPG.Managers;

import RavenMC.ravenRPG.RavenRPG;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RaceItemManager {

    private final RavenRPG plugin;
    private final NamespacedKey abilityItemKey;
    private final NamespacedKey abilityIndexKey;
    private final NamespacedKey abilityIdKey;

    // Hotbar slots for ability items (0-8)
    public static final int PRIMARY_ABILITY_SLOT = 7;
    public static final int SECONDARY_ABILITY_SLOT = 8;

    public RaceItemManager(RavenRPG plugin) {
        this.plugin = plugin;
        this.abilityItemKey = new NamespacedKey(plugin, "ability_item");
        this.abilityIndexKey = new NamespacedKey(plugin, "ability_index");
        this.abilityIdKey = new NamespacedKey(plugin, "ability_id");
    }

    /**
     * Give or update race ability items for a player
     * @param player The player to give items to
     */
    public void setupPlayerAbilityItems(Player player) {
        if (player == null) return;

        UUID playerId = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(playerId);

        if (playerData == null) return;

        String raceName = playerData.getRace();
        Race race = plugin.getRaceManager().getRaces().get(raceName.toLowerCase());

        if (race == null) return;

        List<RaceAbility> abilities = race.getAbilities();
        if (abilities == null || abilities.isEmpty()) return;

        // Create and give primary ability item (first ability)
        if (abilities.size() > 0) {
            RaceAbility primaryAbility = abilities.get(0);
            ItemStack primaryItem = createAbilityItem(race, primaryAbility, 0);
            setItemToSlot(player, primaryItem, PRIMARY_ABILITY_SLOT);
        }

        // Create and give secondary ability item (second ability if exists)
        if (abilities.size() > 1) {
            RaceAbility secondaryAbility = abilities.get(1);
            ItemStack secondaryItem = createAbilityItem(race, secondaryAbility, 1);
            setItemToSlot(player, secondaryItem, SECONDARY_ABILITY_SLOT);
        }
    }

    /**
     * Create an ability item with appropriate appearance and metadata
     */
    private ItemStack createAbilityItem(Race race, RaceAbility ability, int abilityIndex) {
        // Choose material based on race and ability
        Material material = getAbilityMaterial(race.getId(), ability.getId());

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Set name and lore
        meta.setDisplayName(ChatColor.GOLD + ability.getName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.WHITE + ability.getDescription());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Mana Cost: " + ChatColor.AQUA + ability.getManaCost());
        lore.add(ChatColor.YELLOW + "Cooldown: " + ChatColor.WHITE + ability.getCooldown() + " seconds");
        lore.add("");
        lore.add(ChatColor.GREEN + "Right-click to activate");

        meta.setLore(lore);

        // Hide attributes for cleaner appearance
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // Add glowing effect
        meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);

        // Store ability data
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(abilityItemKey, PersistentDataType.BYTE, (byte) 1);
        container.set(abilityIndexKey, PersistentDataType.INTEGER, abilityIndex);
        container.set(abilityIdKey, PersistentDataType.STRING, ability.getId());

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Get appropriate material for ability item based on race and ability
     */
    private Material getAbilityMaterial(String raceId, String abilityId) {
        // Human abilities
        if (raceId.equalsIgnoreCase("human")) {
            if (abilityId.equalsIgnoreCase("adaptability")) {
                return Material.TURTLE_HELMET;
            } else if (abilityId.equalsIgnoreCase("versatility")) {
                return Material.NETHER_STAR;
            }
        }

        // Elf abilities
        else if (raceId.equalsIgnoreCase("elf")) {
            if (abilityId.equalsIgnoreCase("nature_bond")) {
                return Material.GOLDEN_APPLE;
            } else if (abilityId.equalsIgnoreCase("swift_step")) {
                return Material.FEATHER;
            }
        }

        // Orc abilities
        else if (raceId.equalsIgnoreCase("orc")) {
            if (abilityId.equalsIgnoreCase("battle_cry")) {
                return Material.DIAMOND_AXE;
            } else if (abilityId.equalsIgnoreCase("thick_skin")) {
                return Material.SHIELD;
            }
        }

        // Vampire abilities
        else if (raceId.equalsIgnoreCase("vampire")) {
            if (abilityId.equalsIgnoreCase("blood_drain")) {
                return Material.GHAST_TEAR;
            } else if (abilityId.equalsIgnoreCase("night_vision")) {
                return Material.ENDER_EYE;
            }
        }

        // Dwarf abilities
        else if (raceId.equalsIgnoreCase("dwarf")) {
            if (abilityId.equalsIgnoreCase("stone_skin")) {
                return Material.NETHERITE_CHESTPLATE;
            } else if (abilityId.equalsIgnoreCase("hammer_time")) {
                return Material.IRON_AXE;
            }
        }

        // Default fallback
        return Material.BOOK;
    }

    /**
     * Set an item to a specific hotbar slot
     */
    private void setItemToSlot(Player player, ItemStack item, int slot) {
        if (player != null && item != null) {
            player.getInventory().setItem(slot, item);
        }
    }

    /**
     * Check if an item is a race ability item
     */
    public boolean isAbilityItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.has(abilityItemKey, PersistentDataType.BYTE);
    }

    /**
     * Get ability ID from an ability item
     */
    public String getAbilityId(ItemStack item) {
        if (!isAbilityItem(item)) return null;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.get(abilityIdKey, PersistentDataType.STRING);
    }

    /**
     * Get ability index from an ability item
     */
    public int getAbilityIndex(ItemStack item) {
        if (!isAbilityItem(item)) return -1;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.getOrDefault(abilityIndexKey, PersistentDataType.INTEGER, -1);
    }

    /**
     * Update cooldown display on ability items
     */
    public void updateCooldownDisplay(Player player, String abilityId, int remainingCooldown) {
        if (player == null || abilityId == null) return;

        // Find the ability item in the player's inventory
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isAbilityItem(item)) {
                String itemAbilityId = getAbilityId(item);
                if (abilityId.equals(itemAbilityId)) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) continue;

                    List<String> lore = meta.getLore();
                    if (lore == null) continue;

                    // Update cooldown line in lore
                    for (int j = 0; j < lore.size(); j++) {
                        String line = lore.get(j);
                        if (line.contains("Cooldown:")) {
                            if (remainingCooldown > 0) {
                                lore.set(j, ChatColor.RED + "Cooldown: " + remainingCooldown + " seconds remaining");
                            } else {
                                lore.set(j, ChatColor.YELLOW + "Cooldown: Ready");
                            }
                            break;
                        }
                    }

                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    break;
                }
            }
        }
    }
}