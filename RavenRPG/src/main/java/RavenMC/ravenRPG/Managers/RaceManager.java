package RavenMC.ravenRPG.Managers;

import RavenMC.ravenRPG.RavenRPG;
import RavenMC.ravenRPG.Managers.PlayerData;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class RaceManager {

    private final RavenRPG plugin;
    private final Map<String, Race> races;
    private final Map<UUID, Map<String, Long>> abilityCooldowns = new HashMap<>();
    private final OreHighlighter oreHighlighter;

    public RaceManager(RavenRPG plugin) {
        this.plugin = plugin;
        this.races = new HashMap<>();

        // Check if ProtocolLib is available before creating OreHighlighter
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            this.oreHighlighter = new OreHighlighter(plugin);
            plugin.getLogger().info("ProtocolLib found! Dwarf miners_sight ability will use enhanced mode.");
        } else {
            this.oreHighlighter = null;
            plugin.getLogger().warning("ProtocolLib not found! Dwarf miners_sight ability will use fallback mode.");
        }

        // Load races from config
        loadRaces();

        // Start passive effects task
        startPassiveEffectsTask();
    }

    private void loadRaces() {
        ConfigurationSection racesSection = plugin.getConfig().getConfigurationSection("rpg.races");
        if (racesSection == null) {
            plugin.getLogger().warning("No races found in config!");
            // Add default races
            createDefaultRaces();
            return;
        }

        for (String raceId : racesSection.getKeys(false)) {
            ConfigurationSection raceSection = racesSection.getConfigurationSection(raceId);
            if (raceSection == null) continue;

            String name = raceSection.getString("name", raceId);
            String description = raceSection.getString("description", "");
            int baseMana = raceSection.getInt("baseMana", 100);
            int manaRegen = raceSection.getInt("manaRegen", 5);

            List<RaceAbility> abilities = new ArrayList<>();
            ConfigurationSection abilitiesSection = raceSection.getConfigurationSection("abilities");
            if (abilitiesSection != null) {
                for (String abilityId : abilitiesSection.getKeys(false)) {
                    ConfigurationSection abilitySection = abilitiesSection.getConfigurationSection(abilityId);
                    if (abilitySection == null) continue;

                    String abilityName = abilitySection.getString("name", abilityId);
                    String abilityDescription = abilitySection.getString("description", "");
                    int manaCost = abilitySection.getInt("manaCost", 0);
                    int cooldown = abilitySection.getInt("cooldown", 60);

                    RaceAbility ability = new RaceAbility(abilityId, abilityName, abilityDescription, manaCost, cooldown);
                    abilities.add(ability);
                }
            }

            Race race = new Race(raceId, name, description, baseMana, manaRegen, abilities);
            races.put(raceId.toLowerCase(), race);
            plugin.getLogger().info("Loaded race: " + name);
        }
    }

    private void createDefaultRaces() {
        // Human race
        List<RaceAbility> humanAbilities = new ArrayList<>();
        humanAbilities.add(new RaceAbility("adaptability", "Adaptability", "Adapt to gain resistance to current environment", 20, 180));
        humanAbilities.add(new RaceAbility("versatility", "Versatility", "Gain a random positive effect", 25, 300));
        Race human = new Race("human", "Human", "Versatile and adaptable, humans excel at many skills.", 100, 5, humanAbilities);
        races.put("human", human);

        // Elf race
        List<RaceAbility> elfAbilities = new ArrayList<>();
        elfAbilities.add(new RaceAbility("nature_bond", "Nature Bond", "Heal self and nearby plants", 15, 120));
        elfAbilities.add(new RaceAbility("swift_step", "Swift Step", "Gain temporary speed boost", 10, 60));
        Race elf = new Race("elf", "Elf", "Graceful and attuned to nature, elves have enhanced agility and magical affinity.", 120, 7, elfAbilities);
        races.put("elf", elf);

        // Orc race
        List<RaceAbility> orcAbilities = new ArrayList<>();
        orcAbilities.add(new RaceAbility("battle_cry", "Battle Cry", "Gain strength and frighten nearby enemies", 25, 240));
        orcAbilities.add(new RaceAbility("thick_skin", "Thick Skin", "Gain temporary damage resistance", 20, 180));
        Race orc = new Race("orc", "Orc", "Strong and resilient, orcs are natural warriors with enhanced strength.", 80, 4, orcAbilities);
        races.put("orc", orc);

        // Vampire race
        List<RaceAbility> vampireAbilities = new ArrayList<>();
        vampireAbilities.add(new RaceAbility("blood_drain", "Blood Drain", "Steal health from nearby entities", 30, 300));
        vampireAbilities.add(new RaceAbility("night_vision", "Night Vision", "See in the dark", 5, 60));
        Race vampire = new Race("vampire", "Vampire", "Immortal beings of the night with enhanced strength and weaknesses to sunlight.", 90, 6, vampireAbilities);
        races.put("vampire", vampire);

        // Dwarf race
        List<RaceAbility> dwarfAbilities = new ArrayList<>();
        dwarfAbilities.add(new RaceAbility("stone_skin", "Stone Skin", "Gain significant damage resistance", 30, 300));
        dwarfAbilities.add(new RaceAbility("miners_sight", "Miner's Sight", "Detect nearby ores", 15, 120));
        Race dwarf = new Race("dwarf", "Dwarf", "Hardy mountain folk with natural mining and crafting aptitude.", 90, 5, dwarfAbilities);
        races.put("dwarf", dwarf);

        plugin.getLogger().info("Created default races");
    }

    private void startPassiveEffectsTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID playerId = player.getUniqueId();
                    PlayerData playerData = plugin.getPlayerManager().getPlayerData(playerId);

                    if (playerData == null) continue;

                    // Apply race-specific passive effects
                    applyRacePassiveEffects(player, playerData);

                    // Handle mana regeneration
                    regenerateMana(player, playerData);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
    }

    private void applyRacePassiveEffects(Player player, PlayerData playerData) {
        String raceName = playerData.getRace();
        Race race = races.get(raceName.toLowerCase());

        if (race == null) return;

        // Apply race-specific effects
        switch (race.getId()) {
            case "vampire":
                // Vampires are harmed by sunlight
                if (player.getWorld().getTime() < 12000 || player.getWorld().getTime() > 24000) { // Daytime
                    if (player.getLocation().getBlock().getLightFromSky() > 11) { // Direct sunlight
                        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0, false, false));

                        // Every 5 seconds, apply damage in direct sunlight (infrequent to avoid being too annoying)
                        if (System.currentTimeMillis() % 5000 < 1000) {
                            player.damage(1.0); // Half a heart
                            player.sendMessage(ChatColor.RED + "The sunlight burns your vampire skin!");
                        }
                    }
                } else {
                    // Vampires get night vision and strength at night
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 400, 0, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 40, 0, false, false));
                }
                break;

            case "elf":
                // Elves get speed when in forests or lush areas
                Material blockBelow = player.getLocation().getBlock().getRelative(0, -1, 0).getType();
                if (blockBelow == Material.GRASS_BLOCK || blockBelow == Material.DIRT) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, false, false));
                }
                break;

            case "orc":
                // Orcs naturally have more health
                if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null &&
                        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() < 22) { // 11 hearts instead of 10
                    player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(22);
                }
                break;

            case "dwarf":
                // Dwarves get haste when mining
                if (player.getInventory().getItemInMainHand().getType().name().endsWith("PICKAXE")) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 40, 0, false, false));
                }
                break;
        }
    }

    private void regenerateMana(Player player, PlayerData playerData) {
        String raceName = playerData.getRace();
        Race race = races.get(raceName.toLowerCase());

        if (race == null) return;

        int currentMana = playerData.getMana();
        int maxMana = playerData.getMaxMana();
        int manaRegen = race.getManaRegen();

        // Adjust mana regen based on skill level
        manaRegen += playerData.getSkillLevel("magic") / 5;

        if (currentMana < maxMana) {
            playerData.setMana(Math.min(currentMana + manaRegen, maxMana));

            // Update action bar with mana info (if we want to show it)
            // This would require a separate action bar manager to handle multiple messages
        }
    }

    public void setPlayerRace(Player player, String raceName) {
        Race race = races.get(raceName.toLowerCase());

        if (race == null) {
            player.sendMessage(ChatColor.RED + "Invalid race: " + raceName);
            return;
        }

        UUID playerId = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(playerId);

        // Update player data
        playerData.setRace(race.getId());
        playerData.setMaxMana(race.getBaseMana());
        playerData.setMana(race.getBaseMana());

        // Save changes
        plugin.getPlayerManager().savePlayerData(playerId);

        // Update nametag to show new race
        plugin.getServer().getPluginManager().callEvent(new PlayerRaceChangeEvent(player, race.getId()));

        player.sendMessage(ChatColor.GREEN + "You are now a " + race.getName() + "!");
        player.sendMessage(ChatColor.YELLOW + race.getDescription());
    }

    /**
     * Checks if an ability is on cooldown
     * @param playerId Player UUID
     * @param abilityId Ability ID
     * @return true if on cooldown, false if ready to use
     */
    public boolean isOnCooldown(UUID playerId, String abilityId) {
        if (!abilityCooldowns.containsKey(playerId)) {
            return false;
        }

        Map<String, Long> playerCooldowns = abilityCooldowns.get(playerId);
        if (!playerCooldowns.containsKey(abilityId)) {
            return false;
        }

        long lastUsed = playerCooldowns.get(abilityId);
        RaceAbility ability = getRaceAbility(playerId, abilityId);
        if (ability == null) return false;

        long cooldownTime = ability.getCooldown() * 1000; // Convert to milliseconds
        return System.currentTimeMillis() - lastUsed < cooldownTime;
    }

    /**
     * Gets remaining cooldown time in seconds
     * @param playerId Player UUID
     * @param abilityId Ability ID
     * @return Remaining cooldown in seconds, 0 if not on cooldown
     */
    public int getRemainingCooldown(UUID playerId, String abilityId) {
        if (!abilityCooldowns.containsKey(playerId)) {
            return 0;
        }

        Map<String, Long> playerCooldowns = abilityCooldowns.get(playerId);
        if (!playerCooldowns.containsKey(abilityId)) {
            return 0;
        }

        long lastUsed = playerCooldowns.get(abilityId);
        RaceAbility ability = getRaceAbility(playerId, abilityId);
        if (ability == null) return 0;

        long cooldownTime = ability.getCooldown() * 1000; // Convert to milliseconds
        long remaining = cooldownTime - (System.currentTimeMillis() - lastUsed);
        return remaining > 0 ? (int)(remaining / 1000) : 0;
    }

    /**
     * Sets cooldown for an ability
     * @param playerId Player UUID
     * @param abilityId Ability ID
     */
    private void setCooldown(UUID playerId, String abilityId) {
        if (!abilityCooldowns.containsKey(playerId)) {
            abilityCooldowns.put(playerId, new HashMap<>());
        }

        abilityCooldowns.get(playerId).put(abilityId, System.currentTimeMillis());
    }

    /**
     * Helper method to get a race ability by ID
     */
    private RaceAbility getRaceAbility(UUID playerId, String abilityId) {
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(playerId);
        if (playerData == null) return null;

        String raceName = playerData.getRace();
        Race race = races.get(raceName.toLowerCase());
        if (race == null) return null;

        for (RaceAbility ability : race.getAbilities()) {
            if (ability.getId().equalsIgnoreCase(abilityId)) {
                return ability;
            }
        }
        return null;
    }

    public void activateRaceAbility(Player player, String abilityId) {
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        String raceName = playerData.getRace();
        Race race = races.get(raceName.toLowerCase());

        if (race == null) {
            player.sendMessage(ChatColor.RED + "You do not have a valid race!");
            return;
        }

        RaceAbility targetAbility = null;
        for (RaceAbility ability : race.getAbilities()) {
            if (ability.getId().equalsIgnoreCase(abilityId)) {
                targetAbility = ability;
                break;
            }
        }

        if (targetAbility == null) {
            player.sendMessage(ChatColor.RED + "That ability does not exist for your race!");
            return;
        }

        // Check cooldown
        if (isOnCooldown(player.getUniqueId(), abilityId)) {
            int remainingTime = getRemainingCooldown(player.getUniqueId(), abilityId);
            player.sendMessage(ChatColor.RED + "This ability is on cooldown for " +
                    remainingTime + " more seconds!");
            return;
        }

        // Check mana cost
        int manaCost = targetAbility.getManaCost();
        if (playerData.getMana() < manaCost) {
            player.sendMessage(ChatColor.RED + "Not enough mana! You need " + manaCost +
                    " mana to use this ability.");
            return;
        }

        // Activate ability based on race and ability
        boolean success = executeRaceAbility(player, race, targetAbility);

        if (success) {
            // Deduct mana and set cooldown
            playerData.setMana(playerData.getMana() - manaCost);
            setCooldown(player.getUniqueId(), abilityId);
            plugin.getPlayerManager().savePlayerData(player.getUniqueId());

            player.sendMessage(ChatColor.GREEN + "You used " + targetAbility.getName() + "!");

            // Play sound and particle effects for feedback
            player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.0f);
            player.spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

            // Update cooldown display on item - with null safety
            if (plugin.getRaceItemManager() != null) {
                plugin.getRaceItemManager().updateCooldownDisplay(player, abilityId, targetAbility.getCooldown());
            }
        }
    }

    private boolean executeRaceAbility(Player player, Race race, RaceAbility ability) {
        switch (race.getId() + ":" + ability.getId()) {
            // HUMAN ABILITIES
            case "human:adaptability":
                return handleHumanAdaptability(player);
            case "human:versatility":
                return handleHumanVersatility(player);

            // ELF ABILITIES
            case "elf:nature_bond":
                return handleElfNatureBond(player);
            case "elf:swift_step":
                return handleElfSwiftStep(player);

            // ORC ABILITIES
            case "orc:battle_cry":
                return handleOrcBattleCry(player);
            case "orc:thick_skin":
                return handleOrcThickSkin(player);

            // VAMPIRE ABILITIES
            case "vampire:blood_drain":
                return handleVampireBloodDrain(player);
            case "vampire:night_vision":
                return handleVampireNightVision(player);

            // DWARF ABILITIES
            case "dwarf:stone_skin":
                return handleDwarfStoneSkin(player);
            case "dwarf:miners_sight":
                return handleDwarfMinersSight(player);

            default:
                player.sendMessage(ChatColor.RED + "This ability is not yet implemented.");
                return false;
        }
    }

    // HUMAN ABILITY IMPLEMENTATIONS
    private boolean handleHumanAdaptability(Player player) {
        // Adapt to current environment
        if (player.getFireTicks() > 0) {
            // Fire resistance
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 30 * 20, 0));
            player.sendMessage(ChatColor.GOLD + "You adapt to resist fire!");
        } else if (player.getLocation().getBlock().getTemperature() < 0.2) {
            // Cold resistance
            player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30 * 20, 0));
            player.sendMessage(ChatColor.AQUA + "You adapt to the cold climate!");
        } else if (player.getLocation().getBlock().getTemperature() > 0.9) {
            // Heat protection
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 30 * 20, 0));
            player.sendMessage(ChatColor.RED + "You adapt to the hot climate!");
        } else {
            // General adaptability
            player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30 * 20, 0));
            player.sendMessage(ChatColor.GREEN + "You adapt to your surroundings!");
        }
        return true;
    }

    private boolean handleHumanVersatility(Player player) {
        // Random positive effect
        PotionEffectType[] positiveEffects = {
                PotionEffectType.SPEED,
                PotionEffectType.INCREASE_DAMAGE,
                PotionEffectType.REGENERATION,
                PotionEffectType.JUMP,
                PotionEffectType.HEAL
        };

        PotionEffectType selectedEffect = positiveEffects[new Random().nextInt(positiveEffects.length)];
        player.addPotionEffect(new PotionEffect(selectedEffect, 30 * 20, 0));

        player.sendMessage(ChatColor.GOLD + "Your versatility grants you a " +
                formatEffectName(selectedEffect.getName()) + " boost!");
        return true;
    }

    // ELF ABILITY IMPLEMENTATIONS
    private boolean handleElfNatureBond(Player player) {
        // Heal self and nearby plants
        double healAmount = 2.0;
        double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
        player.setHealth(newHealth);

        // Heal nearby plants (simplistic approach)
        Location center = player.getLocation();
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block block = center.getBlock().getRelative(x, y, z);
                    if (isPlant(block.getType())) {
                        // Visually indicate healing
                        player.getWorld().spawnParticle(Particle.HEART, block.getLocation(), 3, 0.5, 0.5, 0.5);
                    }
                }
            }
        }

        player.sendMessage(ChatColor.GREEN + "You bond with nature, healing yourself and nearby plants!");
        return true;
    }

    private boolean handleElfSwiftStep(Player player) {
        // Speed boost and jump boost
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30 * 20, 1)); // Speed II
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 30 * 20, 1)); // Jump Boost II

        player.sendMessage(ChatColor.AQUA + "You move with elven grace!");
        return true;
    }

    // ORC ABILITY IMPLEMENTATIONS
    private boolean handleOrcBattleCry(Player player) {
        // Strength boost and scare nearby enemies
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 30 * 20, 1)); // Strength II
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 20, 0)); // Resistance

        // Scare nearby entities
        for (Entity entity : player.getNearbyEntities(8, 8, 8)) {
            if (entity instanceof Monster) {
                Monster monster = (Monster) entity;
                monster.setTarget(null);
                // Push monsters away
                Vector pushVector = entity.getLocation().toVector()
                        .subtract(player.getLocation().toVector())
                        .normalize().multiply(2);
                entity.setVelocity(pushVector);
            }
        }

        // Sound effect
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.8f);
        player.sendMessage(ChatColor.RED + "You unleash a mighty battle cry!");
        return true;
    }

    private boolean handleOrcThickSkin(Player player) {
        // Significant damage resistance
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30 * 20, 2)); // Resistance III

        player.sendMessage(ChatColor.GRAY + "Your skin hardens like stone!");
        return true;
    }

    // VAMPIRE ABILITY IMPLEMENTATIONS
    private boolean handleVampireBloodDrain(Player player) {
        boolean drained = false;
        for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
            if (!(entity instanceof LivingEntity) || entity instanceof Player) continue;

            LivingEntity target = (LivingEntity) entity;
            double damage = 4.0; // 2 hearts of damage
            target.damage(damage, player);

            // Heal the vampire
            double healAmount = 2.0;
            double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
            player.setHealth(newHealth);

            drained = true;
        }

        if (drained) {
            player.sendMessage(ChatColor.DARK_RED + "You drain the life essence of nearby creatures!");
            player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 10 * 20, 0));
        } else {
            player.sendMessage(ChatColor.RED + "No suitable targets nearby to drain!");
        }

        return drained;
    }

    private boolean handleVampireNightVision(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 5 * 60 * 20, 0)); // 5 minutes
        player.sendMessage(ChatColor.DARK_PURPLE + "Your vampiric vision pierces the darkness!");
        return true;
    }

    // DWARF ABILITY IMPLEMENTATIONS
    private boolean handleDwarfStoneSkin(Player player) {
        // Extreme damage resistance
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30 * 20, 3)); // Resistance IV

        player.sendMessage(ChatColor.DARK_GRAY + "Your skin becomes as hard as stone!");
        return true;
    }

    private boolean handleDwarfMinersSight(Player player) {
        Location center = player.getLocation();
        int radius = 16; // Search radius
        List<Block> nearbyOres = findNearbyOres(center, radius);

        if (nearbyOres.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No ores found nearby.");
            return false;
        }

        // Use ProtocolLib method if available, otherwise fallback to particles
        if (oreHighlighter != null) {
            oreHighlighter.highlightOres(player, nearbyOres, 30); // 30 seconds duration
        } else {
            // Fallback method - just highlight with particles
            for (Block oreBlock : nearbyOres) {
                // Show particles at each ore location
                player.getWorld().spawnParticle(
                        Particle.VILLAGER_HAPPY,
                        oreBlock.getLocation().add(0.5, 0.5, 0.5),
                        10, 0.4, 0.4, 0.4, 0
                );
            }

            // Schedule repeating particle task for 30 seconds
            new BukkitRunnable() {
                int ticks = 0;
                final int maxTicks = 30 * 20; // 30 seconds

                @Override
                public void run() {
                    if (!player.isOnline() || ticks >= maxTicks) {
                        cancel();
                        return;
                    }

                    // Every 20 ticks (1 second) show particles
                    if (ticks % 20 == 0) {
                        for (Block oreBlock : nearbyOres) {
                            // Only show particles for ores within 20 blocks
                            if (player.getLocation().distance(oreBlock.getLocation()) <= 20) {
                                player.getWorld().spawnParticle(
                                        Particle.VILLAGER_HAPPY,
                                        oreBlock.getLocation().add(0.5, 0.5, 0.5),
                                        5, 0.4, 0.4, 0.4, 0
                                );
                            }
                        }
                    }

                    ticks += 1;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        player.sendMessage(ChatColor.GOLD + "Your dwarven sight reveals nearby ores! (glowing for 30 seconds)");
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);

        return true;
    }

    // UTILITY METHODS
    private List<Block> findNearbyOres(Location center, int radius) {
        List<Block> ores = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = center.getBlock().getRelative(x, y, z);
                    if (isOre(block.getType())) {
                        ores.add(block);
                    }
                }
            }
        }
        return ores;
    }

    private boolean isOre(Material material) {
        return material.name().contains("ORE") &&
                !material.name().contains("DEEPSLATE_ORE_BLOCK") &&
                !material.name().contains("NETHER_BRICK");
    }

    private boolean isPlant(Material material) {
        return material.name().contains("LEAVES") ||
                material.name().contains("SAPLING") ||
                material.name().contains("GRASS") ||
                material.name().contains("FLOWER") ||
                material.name().contains("VINE") ||
                material.name().contains("MUSHROOM");
    }

    private String formatEffectName(String effectName) {
        return Arrays.stream(effectName.toLowerCase().split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    public Map<String, Race> getRaces() {
        if (races == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(races);
    }
}