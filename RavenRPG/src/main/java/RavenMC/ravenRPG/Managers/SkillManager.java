package RavenMC.ravenRPG.Managers;

import RavenMC.ravenRPG.RavenRPG;
import RavenMC.ravenRPG.Managers.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SkillManager {

    private final RavenRPG plugin;
    private final Map<String, Skill> skills;

    public SkillManager(RavenRPG plugin) {
        this.plugin = plugin;
        this.skills = new HashMap<>();

        // Load skills from config
        loadSkills();
    }

    private void loadSkills() {
        ConfigurationSection skillsSection = plugin.getConfig().getConfigurationSection("rpg.skills");
        if (skillsSection == null) {
            plugin.getLogger().warning("No skills found in config!");
            // Add default skills
            createDefaultSkills();
            return;
        }

        for (String skillId : skillsSection.getKeys(false)) {
            ConfigurationSection skillSection = skillsSection.getConfigurationSection(skillId);
            if (skillSection == null) continue;

            String name = skillSection.getString("name", skillId);
            String description = skillSection.getString("description", "");

            Map<Integer, SkillReward> rewards = new HashMap<>();
            ConfigurationSection rewardsSection = skillSection.getConfigurationSection("rewards");
            if (rewardsSection != null) {
                for (String levelKey : rewardsSection.getKeys(false)) {
                    int level;
                    try {
                        level = Integer.parseInt(levelKey);
                    } catch (NumberFormatException e) {
                        continue;
                    }

                    ConfigurationSection rewardSection = rewardsSection.getConfigurationSection(levelKey);
                    if (rewardSection == null) continue;

                    String type = rewardSection.getString("type", "");
                    String effect = rewardSection.getString("effect", "");

                    SkillReward reward = new SkillReward(type, effect);
                    rewards.put(level, reward);
                }
            }

            Skill skill = new Skill(skillId, name, description, rewards);
            skills.put(skillId.toLowerCase(), skill);
            plugin.getLogger().info("Loaded skill: " + name);
        }
    }

    private void createDefaultSkills() {
        // Mining skill
        Map<Integer, SkillReward> miningRewards = new HashMap<>();
        miningRewards.put(5, new SkillReward("effect", "FAST_DIGGING:1:600")); // Haste I for 30 seconds on mining
        miningRewards.put(10, new SkillReward("effect", "FAST_DIGGING:2:600")); // Haste II for 30 seconds on mining
        miningRewards.put(15, new SkillReward("drop_chance", "DIAMOND:0.05")); // 5% chance for extra diamonds
        miningRewards.put(20, new SkillReward("drop_chance", "DIAMOND:0.1")); // 10% chance for extra diamonds
        Skill mining = new Skill("mining", "Mining", "Increases mining speed and yields", miningRewards);
        skills.put("mining", mining);

        // Combat skill
        Map<Integer, SkillReward> combatRewards = new HashMap<>();
        combatRewards.put(5, new SkillReward("damage_bonus", "0.1")); // +10% damage
        combatRewards.put(10, new SkillReward("damage_bonus", "0.2")); // +20% damage
        combatRewards.put(15, new SkillReward("effect", "INCREASE_DAMAGE:0:200")); // Strength on kill
        combatRewards.put(20, new SkillReward("damage_bonus", "0.3")); // +30% damage
        Skill combat = new Skill("combat", "Combat", "Increases damage dealt and grants combat abilities", combatRewards);
        skills.put("combat", combat);

        // Fishing skill
        Map<Integer, SkillReward> fishingRewards = new HashMap<>();
        fishingRewards.put(5, new SkillReward("catch_speed", "0.1")); // 10% faster catching
        fishingRewards.put(10, new SkillReward("special_catch", "TROPICAL_FISH:0.05")); // Rare fish chance
        fishingRewards.put(15, new SkillReward("special_catch", "NAUTILUS_SHELL:0.02")); // Nautilus shell chance
        fishingRewards.put(20, new SkillReward("special_catch", "TRIDENT:0.01")); // Rare trident chance
        Skill fishing = new Skill("fishing", "Fishing", "Improves fishing speed and rare catches", fishingRewards);
        skills.put("fishing", fishing);

        // Crafting skill
        Map<Integer, SkillReward> craftingRewards = new HashMap<>();
        craftingRewards.put(5, new SkillReward("extra_item", "0.05")); // 5% chance for extra crafted item
        craftingRewards.put(10, new SkillReward("material_save", "0.1")); // 10% chance to not consume materials
        craftingRewards.put(15, new SkillReward("extra_item", "0.1")); // 10% chance for extra crafted item
        craftingRewards.put(20, new SkillReward("special_craft", "ENCHANTED")); // Chance for pre-enchanted items
        Skill crafting = new Skill("crafting", "Crafting", "Improves crafting efficiency and results", craftingRewards);
        skills.put("crafting", crafting);

        // Magic skill (affects mana and spells)
        Map<Integer, SkillReward> magicRewards = new HashMap<>();
        magicRewards.put(5, new SkillReward("max_mana", "10")); // +10 max mana
        magicRewards.put(10, new SkillReward("mana_regen", "1")); // +1 mana regen
        magicRewards.put(15, new SkillReward("spell_power", "0.1")); // +10% spell power
        magicRewards.put(20, new SkillReward("max_mana", "20")); // +20 more max mana
        Skill magic = new Skill("magic", "Magic", "Increases magical power and mana pool", magicRewards);
        skills.put("magic", magic);

        plugin.getLogger().info("Created default skills");
    }

    public void awardSkillXP(Player player, String skillName, int amount) {
        UUID playerId = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(playerId);

        if (playerData == null) return;

        skillName = skillName.toLowerCase();
        Skill skill = skills.get(skillName);

        if (skill == null) {
            plugin.getLogger().warning("Tried to award XP for unknown skill: " + skillName);
            return;
        }

        int currentLevel = playerData.getSkillLevel(skillName);
        int currentXP = playerData.getSkillXP(skillName);

        // Apply race bonuses to XP gain
        String race = playerData.getRace();
        if (race != null) {
            // Race-specific skill bonuses
            if (race.equalsIgnoreCase("elf") && skillName.equals("magic")) {
                amount = (int)(amount * 1.2); // Elves get 20% more magic XP
            } else if (race.equalsIgnoreCase("orc") && skillName.equals("combat")) {
                amount = (int)(amount * 1.15); // Orcs get 15% more combat XP
            } else if (race.equalsIgnoreCase("dwarf") && skillName.equals("mining")) {
                amount = (int)(amount * 1.25); // Dwarves get 25% more mining XP
            }
        }

        int newXP = currentXP + amount;
        int xpForNextLevel = calculateXPForLevel(currentLevel + 1);

        playerData.setSkillXP(skillName, newXP);

        // Check for level up
        if (newXP >= xpForNextLevel) {
            levelUpSkill(player, playerData, skill, currentLevel);
        }

        // Save changes
        plugin.getPlayerManager().savePlayerData(playerId);
    }

    private int calculateXPForLevel(int level) {
        // Simple formula: each level requires more XP
        return 100 * level * level;
    }

    private void levelUpSkill(Player player, PlayerData playerData, Skill skill, int currentLevel) {
        int newLevel = currentLevel + 1;
        String skillName = skill.getId();

        playerData.setSkillLevel(skillName, newLevel);

        // Notify player
        player.sendMessage(ChatColor.GREEN + "Your " + skill.getName() + " skill increased to level " + newLevel + "!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        // Apply rewards for this level
        SkillReward reward = skill.getRewards().get(newLevel);
        if (reward != null) {
            applySkillReward(player, reward, skill.getName());
            player.sendMessage(ChatColor.YELLOW + "You earned a new skill perk: " + reward.getDescription());
        }

        // Check if magic skill increase - update max mana
        if (skillName.equals("magic")) {
            // Increase max mana by 5 per level
            int newMaxMana = playerData.getMaxMana() + 5;
            playerData.setMaxMana(newMaxMana);
            player.sendMessage(ChatColor.AQUA + "Your maximum mana increased to " + newMaxMana + "!");
        }
    }

    private void applySkillReward(Player player, SkillReward reward, String skillName) {
        // This would be expanded based on the reward types
        // For now, just handling a few examples

        String type = reward.getType();
        String effect = reward.getEffect();

        switch (type) {
            case "max_mana":
                int manaBonus = Integer.parseInt(effect);
                PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
                int newMaxMana = playerData.getMaxMana() + manaBonus;
                playerData.setMaxMana(newMaxMana);
                player.sendMessage(ChatColor.AQUA + "Your maximum mana increased by " + manaBonus + "!");
                break;

            case "effect":
                // Format: EFFECT_TYPE:LEVEL:DURATION
                String[] parts = effect.split(":");
                if (parts.length >= 3) {
                    try {
                        org.bukkit.potion.PotionEffectType potionType = org.bukkit.potion.PotionEffectType.getByName(parts[0]);
                        int level = Integer.parseInt(parts[1]);
                        int duration = Integer.parseInt(parts[2]);

                        if (potionType != null) {
                            // This would be used when the skill is actively used, not just on level up
                            reward.setDescription("Gain " + formatEffectName(potionType.getName()) +
                                    " " + (level + 1) + " when using " + skillName);
                        }
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid effect format: " + effect);
                    }
                }
                break;

            case "damage_bonus":
                double damageBonus = Double.parseDouble(effect);
                int percent = (int)(damageBonus * 100);
                reward.setDescription("+" + percent + "% damage with weapons");
                break;

            case "drop_chance":
                // Format: MATERIAL:CHANCE
                String[] dropParts = effect.split(":");
                if (dropParts.length >= 2) {
                    String material = dropParts[0];
                    double chance = Double.parseDouble(dropParts[1]);
                    int chancePercent = (int)(chance * 100);
                    reward.setDescription(chancePercent + "% chance for bonus " + formatMaterialName(material));
                }
                break;

            case "special_catch":
                // Format: ITEM:CHANCE
                String[] catchParts = effect.split(":");
                if (catchParts.length >= 2) {
                    String item = catchParts[0];
                    double chance = Double.parseDouble(catchParts[1]);
                    int chancePercent = (int)(chance * 100);
                    reward.setDescription(chancePercent + "% chance to catch " + formatMaterialName(item));
                }
                break;
        }
    }

    private String formatEffectName(String name) {
        return name.toLowerCase().replace('_', ' ');
    }

    private String formatMaterialName(String materialName) {
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    public Map<String, Skill> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    // Event listener for skill XP gain
    public static class SkillEventListener implements Listener {

        private final RavenRPG plugin;

        public SkillEventListener(RavenRPG plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent event) {
            Player player = event.getPlayer();
            Material material = event.getBlock().getType();

            // Award mining XP for certain blocks
            if (material.name().contains("ORE") || material == Material.STONE ||
                    material == Material.DEEPSLATE || material == Material.ANCIENT_DEBRIS) {

                int xpAmount = 0;
                switch (material) {
                    case COAL_ORE:
                    case COPPER_ORE:
                        xpAmount = 3;
                        break;
                    case IRON_ORE:
                    case DEEPSLATE_IRON_ORE:
                        xpAmount = 5;
                        break;
                    case GOLD_ORE:
                    case DEEPSLATE_GOLD_ORE:
                    case NETHER_GOLD_ORE:
                        xpAmount = 8;
                        break;
                    case REDSTONE_ORE:
                    case DEEPSLATE_REDSTONE_ORE:
                    case LAPIS_ORE:
                    case DEEPSLATE_LAPIS_ORE:
                        xpAmount = 10;
                        break;
                    case DIAMOND_ORE:
                    case DEEPSLATE_DIAMOND_ORE:
                        xpAmount = 15;
                        break;
                    case EMERALD_ORE:
                    case DEEPSLATE_EMERALD_ORE:
                        xpAmount = 20;
                        break;
                    case ANCIENT_DEBRIS:
                        xpAmount = 30;
                        break;
                    default:
                        xpAmount = 1; // Stone, deepslate, etc.
                }

                // Award XP
                plugin.getSkillManager().awardSkillXP(player, "mining", xpAmount);

                // Apply mining perks
                applyMiningPerks(player, material);
            }
        }

        private void applyMiningPerks(Player player, Material material) {
            UUID playerId = player.getUniqueId();
            PlayerData playerData = plugin.getPlayerManager().getPlayerData(playerId);

            if (playerData == null) return;

            int miningLevel = playerData.getSkillLevel("mining");

            // Apply mining perks based on level
            if (miningLevel >= 5) {
                // Chance for faster mining (Haste effect)
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.FAST_DIGGING,
                        600, // 30 seconds
                        miningLevel >= 10 ? 1 : 0 // Haste II at level 10+
                ));
            }

            // Chance for double drops at higher levels
            if (miningLevel >= 15 && material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE) {
                double chance = miningLevel >= 20 ? 0.1 : 0.05; // 5% at level 15, 10% at level 20

                if (Math.random() < chance) {
                    // Drop an extra diamond
                    player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.DIAMOND));
                    player.sendMessage(ChatColor.AQUA + "Your mining skill helped you find an extra diamond!");
                }
            }
        }

        @EventHandler
        public void onEntityKill(EntityDeathEvent event) {
            if (event.getEntity().getKiller() != null) {
                Player player = event.getEntity().getKiller();

                // Award combat XP based on entity type
                int xpAmount = 0;
                switch (event.getEntityType()) {
                    case ZOMBIE:
                    case SKELETON:
                    case SPIDER:
                        xpAmount = 5;
                        break;
                    case CREEPER:
                    case WITCH:
                    case DROWNED:
                        xpAmount = 8;
                        break;
                    case BLAZE:
                    case ENDERMAN:
                    case PIGLIN_BRUTE:
                        xpAmount = 12;
                        break;
                    case WITHER_SKELETON:
                    case RAVAGER:
                        xpAmount = 15;
                        break;
                    case WITHER:
                    case ENDER_DRAGON:
                        xpAmount = 100;
                        break;
                    default:
                        if (event.getEntityType().isAlive()) {
                            xpAmount = 3; // Default for living entities
                        }
                }

                if (xpAmount > 0) {
                    plugin.getSkillManager().awardSkillXP(player, "combat", xpAmount);

                    // Apply combat perks
                    applyCombatPerks(player);
                }
            }
        }

        private void applyCombatPerks(Player player) {
            UUID playerId = player.getUniqueId();
            PlayerData playerData = plugin.getPlayerManager().getPlayerData(playerId);

            if (playerData == null) return;

            int combatLevel = playerData.getSkillLevel("combat");

            // Apply combat perks based on level
            if (combatLevel >= 15) {
                // Chance for Strength effect on kill at level 15+
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.INCREASE_DAMAGE,
                        200, // 10 seconds
                        0    // Strength I
                ));
            }
        }

        @EventHandler
        public void onFish(PlayerFishEvent event) {
            if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
                Player player = event.getPlayer();

                // Award fishing XP
                plugin.getSkillManager().awardSkillXP(player, "fishing", 10);

                // Apply fishing perks
                applyFishingPerks(player, event);
            }
        }

        private void applyFishingPerks(Player player, PlayerFishEvent event) {
            UUID playerId = player.getUniqueId();
            PlayerData playerData = plugin.getPlayerManager().getPlayerData(playerId);

            if (playerData == null) return;

            int fishingLevel = playerData.getSkillLevel("fishing");

            // Special catches at higher levels
            if (fishingLevel >= 10) {
                // Chance for special catches based on level
                double specialChance = 0;
                Material specialCatch = null;

                if (fishingLevel >= 20 && Math.random() < 0.01) {
                    // 1% chance for a trident at level 20+
                    specialChance = 0.01;
                    specialCatch = Material.TRIDENT;
                } else if (fishingLevel >= 15 && Math.random() < 0.02) {
                    // 2% chance for a nautilus shell at level 15+
                    specialChance = 0.02;
                    specialCatch = Material.NAUTILUS_SHELL;
                } else if (fishingLevel >= 10 && Math.random() < 0.05) {
                    // 5% chance for a tropical fish at level 10+
                    specialChance = 0.05;
                    specialCatch = Material.TROPICAL_FISH;
                }

                if (specialCatch != null) {
                    // Replace the caught item with a special catch
                    if (event.getCaught() instanceof org.bukkit.entity.Item) {
                        ((org.bukkit.entity.Item) event.getCaught()).setItemStack(new ItemStack(specialCatch));
                        player.sendMessage(ChatColor.AQUA + "Your fishing skill helped you catch something special!");
                    }
                }
            }
        }

        @EventHandler
        public void onCraft(CraftItemEvent event) {
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                ItemStack result = event.getRecipe().getResult();

                // Award crafting XP based on item complexity
                int xpAmount = 0;

                // Simple crafting categories
                if (result.getType().name().contains("WOODEN_")) {
                    xpAmount = 2;
                } else if (result.getType().name().contains("STONE_")) {
                    xpAmount = 3;
                } else if (result.getType().name().contains("IRON_")) {
                    xpAmount = 5;
                } else if (result.getType().name().contains("GOLDEN_")) {
                    xpAmount = 7;
                } else if (result.getType().name().contains("DIAMOND_")) {
                    xpAmount = 10;
                } else if (result.getType().name().contains("NETHERITE_")) {
                    xpAmount = 15;
                } else {
                    // Default value for other items
                    xpAmount = 1;
                }

                if (xpAmount > 0) {
                    plugin.getSkillManager().awardSkillXP(player, "crafting", xpAmount);

                    // Apply crafting perks
                    applyCraftingPerks(player, event);
                }
            }
        }

        private void applyCraftingPerks(Player player, CraftItemEvent event) {
            UUID playerId = player.getUniqueId();
            PlayerData playerData = plugin.getPlayerManager().getPlayerData(playerId);

            if (playerData == null) return;

            int craftingLevel = playerData.getSkillLevel("crafting");

            // Extra item chance at level 5+
            if (craftingLevel >= 5) {
                double extraChance = craftingLevel >= 15 ? 0.1 : 0.05; // 5% at level 5, 10% at level 15

                if (Math.random() < extraChance) {
                    // Give an extra of the crafted item
                    ItemStack result = event.getRecipe().getResult().clone();
                    player.getInventory().addItem(result);
                    player.sendMessage(ChatColor.GREEN + "Your crafting skill produced an extra item!");
                }
            }

            // Material saving chance at level 10+
            if (craftingLevel >= 10) {
                double saveChance = 0.1; // 10% chance to not consume materials

                if (Math.random() < saveChance) {
                    // Would need to manipulate the crafting inventory to save materials
                    // This is complex and would require additional handling
                    player.sendMessage(ChatColor.GREEN + "Your crafting skill saved some materials!");
                }
            }

            // Special crafting at level 20+
            if (craftingLevel >= 20) {
                ItemStack result = event.getRecipe().getResult();

                // Only apply to tools and weapons
                if (result.getType().name().endsWith("_SWORD") ||
                        result.getType().name().endsWith("_AXE") ||
                        result.getType().name().endsWith("_PICKAXE") ||
                        result.getType().name().endsWith("_SHOVEL") ||
                        result.getType().name().endsWith("_HOE")) {

                    double enchantChance = 0.05; // 5% chance to enchant

                    if (Math.random() < enchantChance) {
                        // Add a random appropriate enchantment
                        org.bukkit.inventory.meta.ItemMeta meta = result.getItemMeta();
                        if (meta != null) {
                            // This would be expanded with proper enchantment selection
                            if (result.getType().name().endsWith("_SWORD")) {
                                meta.addEnchant(org.bukkit.enchantments.Enchantment.DAMAGE_ALL, 1, false);
                            } else if (result.getType().name().endsWith("_PICKAXE")) {
                                meta.addEnchant(org.bukkit.enchantments.Enchantment.DIG_SPEED, 1, false);
                            }
                            result.setItemMeta(meta);

                            // Would need to replace the crafting result with the enchanted version
                            // This is complex and would require additional handling
                            player.sendMessage(ChatColor.GOLD + "Your crafting mastery enchanted your creation!");
                        }
                    }
                }
            }
        }
    }
}

