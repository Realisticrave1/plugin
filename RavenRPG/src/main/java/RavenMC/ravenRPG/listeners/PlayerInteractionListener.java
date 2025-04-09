package RavenMC.ravenRPG.listeners;

import RavenMC.ravenRPG.RavenRPG;
import RavenMC.ravenRPG.Managers.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerInteractionListener implements Listener {

    private final RavenRPG plugin;
    private final Map<UUID, Long> lastManaWarning = new HashMap<>();
    private static final long MANA_WARNING_COOLDOWN = 5000; // 5 seconds in milliseconds

    public PlayerInteractionListener(RavenRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Check if a player is dealing damage
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());

        // Apply race-specific damage modifiers
        String race = playerData.getRace();
        double damageModifier = 1.0;

        switch (race.toLowerCase()) {
            case "orc":
                // Orcs deal more damage with melee weapons
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (isMeleeWeapon(itemInHand.getType())) {
                    damageModifier = 1.15; // 15% more damage
                }
                break;

            case "vampire":
                // Vampires deal more damage at night, less during day
                long time = player.getWorld().getTime();
                if (time >= 13000 && time <= 23000) { // Night time
                    damageModifier = 1.2; // 20% more damage at night
                } else if (player.getLocation().getBlock().getLightFromSky() > 11) {
                    damageModifier = 0.9; // 10% less damage in direct sunlight
                }
                break;

            case "elf":
                // Elves deal more damage with bow and arrow
                if (player.getInventory().getItemInMainHand().getType() == Material.BOW) {
                    damageModifier = 1.1; // 10% more damage with bows
                }
                break;
        }

        // Apply skill-based damage modifiers
        int combatLevel = playerData.getSkillLevel("combat");
        if (combatLevel > 0) {
            // Add damage based on combat level
            double skillBonus = 0.0;

            if (combatLevel >= 20) {
                skillBonus = 0.3; // 30% at level 20+
            } else if (combatLevel >= 10) {
                skillBonus = 0.2; // 20% at level 10-19
            } else if (combatLevel >= 5) {
                skillBonus = 0.1; // 10% at level 5-9
            }

            damageModifier += skillBonus;
        }

        // Apply damage modifier
        event.setDamage(event.getDamage() * damageModifier);

        // Award combat XP on hit (small amount)
        if (event.getEntity() instanceof LivingEntity && !(event.getEntity() instanceof Player)) {
            // Only award XP for hitting mobs, not players
            plugin.getSkillManager().awardSkillXP(player, "combat", 1);
        }

        // Special race abilities on hit
        if (race.equalsIgnoreCase("vampire") && event.getEntity() instanceof LivingEntity) {
            // Vampires have a chance to steal health on hit
            double vampireChance = 0.15; // 15% chance

            if (Math.random() < vampireChance) {
                double healAmount = event.getDamage() * 0.2; // 20% of damage dealt
                double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
                player.setHealth(newHealth);

                // Visual feedback
                player.sendMessage(ChatColor.DARK_RED + "You drained life essence from your victim!");
            }
        }
    }

    private boolean isMeleeWeapon(Material material) {
        return material == Material.WOODEN_SWORD || material == Material.STONE_SWORD ||
                material == Material.IRON_SWORD || material == Material.GOLDEN_SWORD ||
                material == Material.DIAMOND_SWORD || material == Material.NETHERITE_SWORD ||
                material == Material.WOODEN_AXE || material == Material.STONE_AXE ||
                material == Material.IRON_AXE || material == Material.GOLDEN_AXE ||
                material == Material.DIAMOND_AXE || material == Material.NETHERITE_AXE;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        // Check if a player killed the entity
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        // This event will be handled by the SkillEventListener for skill XP

        // Handle special race-based effects on kill
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(killer.getUniqueId());
        String race = playerData.getRace();

        if (race.equalsIgnoreCase("vampire")) {
            // Vampires get more mana on kill
            int manaGain = 5;
            int newMana = Math.min(playerData.getMana() + manaGain, playerData.getMaxMana());
            playerData.setMana(newMana);

            // Notify player if significant mana gain
            if (manaGain >= 5) {
                killer.sendMessage(ChatColor.DARK_PURPLE + "Killing restores " + manaGain + " mana!");
            }
        }

        // Some races might have special drop chances
        if (race.equalsIgnoreCase("orc") && Math.random() < 0.1) { // 10% chance
            // Orcs occasionally get bonus drops from kills
            event.getDrops().forEach(item -> item.setAmount(item.getAmount() + 1));
            killer.sendMessage(ChatColor.RED + "Your orcish strength yields extra loot!");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Dismiss raven when player dies
        plugin.getRavenManager().destroyRaven(player.getUniqueId());

        // Vampires burn to ash when killed in sunlight
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (playerData.getRace().equalsIgnoreCase("vampire")) {
            // Check if died during daytime in direct sunlight
            if (player.getWorld().getTime() < 13000 && player.getLocation().getBlock().getLightFromSky() > 11) {
                event.setDeathMessage(player.getName() + " burned to ash in the sunlight");
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Handle case where a player tries to use an item or skill that requires mana
        // but doesn't have enough mana - just as an example

        // For example, using a magical item might require mana
        if (event.hasItem() && isMagicalItem(event.getItem())) {
            PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
            int manaCost = getMagicalItemManaCost(event.getItem());

            if (playerData.getMana() < manaCost) {
                // Cancel the event
                event.setCancelled(true);

                // Notify player, but not too often to avoid spam
                if (canSendManaWarning(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Not enough mana! You need " + manaCost +
                            " mana to use this item.");
                    lastManaWarning.put(player.getUniqueId(), System.currentTimeMillis());
                }

                return;
            }

            // Consume mana
            playerData.setMana(playerData.getMana() - manaCost);
        }
    }

    private boolean canSendManaWarning(UUID playerId) {
        if (!lastManaWarning.containsKey(playerId)) {
            return true;
        }

        long lastTime = lastManaWarning.get(playerId);
        return System.currentTimeMillis() - lastTime >= MANA_WARNING_COOLDOWN;
    }

    private boolean isMagicalItem(ItemStack item) {
        // This is a placeholder - you would implement your own magical item detection
        // For example, based on lore, custom model data, or specific enchantments
        if (item == null) return false;

        // Example: Items with glow effect (enchanted)
        return item.getEnchantments().size() > 0;
    }

    private int getMagicalItemManaCost(ItemStack item) {
        // This is a placeholder - you would implement your own mana cost calculation
        // For example, based on item type, level, or metadata

        // Example: Base cost of 10 mana, plus 5 per enchantment level
        int cost = 10;
        if (item != null) {
            for (int level : item.getEnchantments().values()) {
                cost += level * 5;
            }
        }

        return cost;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());

        // Handle food effects based on race
        if (event.getItem().getType().isEdible()) {
            String race = playerData.getRace();

            // Different races might get different effects from food
            switch (race.toLowerCase()) {
                case "dwarf":
                    // Dwarves get more saturation from food
                    if (player.getFoodLevel() < 20) {
                        player.setSaturation(player.getSaturation() + 2.0f);
                    }
                    break;

                case "vampire":
                    // Vampires don't benefit as much from normal food
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "As a vampire, you crave blood, not food!");

                    // Reduce food level for trying to eat normal food
                    int newFoodLevel = Math.max(player.getFoodLevel() - 1, 0);
                    player.setFoodLevel(newFoodLevel);
                    break;

                case "elf":
                    // Elves get mana from plant-based foods
                    if (isPlantBasedFood(event.getItem().getType())) {
                        int manaGain = 5;
                        int newMana = Math.min(playerData.getMana() + manaGain, playerData.getMaxMana());
                        playerData.setMana(newMana);
                        player.sendMessage(ChatColor.GREEN + "The plant essence restores " + manaGain + " mana!");
                    }
                    break;
            }
        }
    }

    private boolean isPlantBasedFood(Material material) {
        return material == Material.APPLE || material == Material.GOLDEN_APPLE ||
                material == Material.ENCHANTED_GOLDEN_APPLE || material == Material.SWEET_BERRIES ||
                material == Material.GLOW_BERRIES || material == Material.CARROT ||
                material == Material.GOLDEN_CARROT || material == Material.POTATO ||
                material == Material.BAKED_POTATO || material == Material.BEETROOT ||
                material == Material.BEETROOT_SOUP || material == Material.DRIED_KELP ||
                material == Material.MELON_SLICE;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerCraft(CraftItemEvent event) {
        // This event will be handled by the SkillEventListener for crafting skill XP

        // But we can add race-specific crafting bonuses here
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        String race = playerData.getRace();

        if (race.equalsIgnoreCase("dwarf") && isToolOrWeapon(event.getRecipe().getResult().getType())) {
            // Dwarves have a chance to craft higher durability items
            double chance = 0.25; // 25% chance

            if (Math.random() < chance) {
                // This would be implemented with a custom crafting result
                // For simplicity, just notify the player
                player.sendMessage(ChatColor.GOLD + "Your dwarven crafting skill creates a more durable item!");
            }
        }
    }

    private boolean isToolOrWeapon(Material material) {
        String name = material.name();
        return name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL") ||
                name.endsWith("_HOE") || name.endsWith("_SWORD") || name.equals("BOW") ||
                name.equals("CROSSBOW") || name.equals("TRIDENT") || name.equals("SHIELD");
    }
}