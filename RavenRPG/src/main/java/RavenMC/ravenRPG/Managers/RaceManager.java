package RavenMC.ravenRPG.Managers;

import RavenMC.ravenRPG.RavenRPG;
import RavenMC.ravenRPG.Managers.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class RaceManager {

    private final RavenRPG plugin;
    private final Map<String, Race> races;
    private final Map<UUID, Long> raceCooldowns = new HashMap<>();

    public RaceManager(RavenRPG plugin) {
        this.plugin = plugin;
        this.races = new HashMap<>();

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

    public void activateRaceAbility(Player player, String abilityId) {
        UUID playerId = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(playerId);

        if (playerData == null) {
            player.sendMessage(ChatColor.RED + "Error accessing player data!");
            return;
        }

        String raceName = playerData.getRace();
        Race race = races.get(raceName.toLowerCase());

        if (race == null) {
            player.sendMessage(ChatColor.RED + "You do not have a valid race!");
            return;
        }

        // Find the ability
        RaceAbility ability = null;
        for (RaceAbility a : race.getAbilities()) {
            if (a.getId().equalsIgnoreCase(abilityId)) {
                ability = a;
                break;
            }
        }

        if (ability == null) {
            player.sendMessage(ChatColor.RED + "That ability does not exist for your race!");
            return;
        }

        // Check cooldown
        String abilityKey = raceName + ":" + abilityId;
        if (raceCooldowns.containsKey(playerId)) {
            long lastUsed = raceCooldowns.get(playerId);
            long cooldownTime = ability.getCooldown() * 1000L; // Convert to milliseconds

            if (System.currentTimeMillis() - lastUsed < cooldownTime) {
                long remainingSeconds = (cooldownTime - (System.currentTimeMillis() - lastUsed)) / 1000;
                player.sendMessage(ChatColor.RED + "This ability is on cooldown for " + remainingSeconds + " more seconds!");
                return;
            }
        }

        // Check mana cost
        int manaCost = ability.getManaCost();
        if (playerData.getMana() < manaCost) {
            player.sendMessage(ChatColor.RED + "Not enough mana! You need " + manaCost + " mana to use this ability.");
            return;
        }

        // Use mana
        playerData.setMana(playerData.getMana() - manaCost);

        // Apply ability effect based on race and ability
        boolean success = applyRaceAbility(player, race, ability);

        if (success) {
            // Apply cooldown
            raceCooldowns.put(playerId, System.currentTimeMillis());
            player.sendMessage(ChatColor.GREEN + "You used " + ability.getName() + "!");
        }
    }

    private boolean applyRaceAbility(Player player, Race race, RaceAbility ability) {
        // This would be expanded for all abilities of each race
        // For now, implementing a few examples

        switch (race.getId() + ":" + ability.getId()) {
            case "human:adaptability":
                // Gain resistance based on environment
                if (player.getFireTicks() > 0) {
                    // In fire - grant fire resistance
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 30 * 20, 0));
                    player.sendMessage(ChatColor.GOLD + "You adapt to resist fire!");
                } else if (player.getLocation().getBlock().getTemperature() < 0.2) {
                    // Cold biome - warmth
                    player.setFireTicks(0);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30 * 20, 0));
                    player.sendMessage(ChatColor.AQUA + "You adapt to the cold climate!");
                } else if (player.getLocation().getBlock().getTemperature() > 0.9) {
                    // Hot biome - cooling
                    player.setFireTicks(0);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 60 * 20, 0));
                    player.sendMessage(ChatColor.RED + "You adapt to the hot climate!");
                } else {
                    // Default - general resistance
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30 * 20, 0));
                    player.sendMessage(ChatColor.GREEN + "You adapt to your surroundings!");
                }
                return true;

            case "elf:swift_step":
                // Speed boost for elves
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30 * 20, 1));
                player.sendMessage(ChatColor.GREEN + "You move with elven grace!");
                return true;

            case "vampire:blood_drain":
                // Vampires drain health from nearby entities
                boolean drained = false;
                for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 5, 5, 5)) {
                    if (!(entity instanceof LivingEntity) || entity instanceof Player) continue; // Skip non-living entities and players

                    LivingEntity livingEntity = (LivingEntity) entity;
                    livingEntity.damage(4.0, player); // 2 hearts of damage

                    // Heal the vampire
                    double newHealth = Math.min(player.getHealth() + 2.0, player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                    player.setHealth(newHealth);

                    player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 10 * 20, 0));
                    drained = true;
                }

                if (drained) {
                    player.sendMessage(ChatColor.DARK_RED + "You drain the life essence of nearby creatures!");
                } else {
                    player.sendMessage(ChatColor.RED + "There are no suitable targets nearby to drain!");
                    // Refund half the mana
                    PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
                    playerData.setMana(playerData.getMana() + (ability.getManaCost() / 2));
                }
                return drained;

            case "orc:battle_cry":
                // Orcs gain strength and scare enemies
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 20, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 10 * 20, 0));

                for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 8, 8, 8)) {
                    if (!(entity instanceof LivingEntity) || entity instanceof Player) continue;

                    // Make mobs run away
                    if (entity instanceof org.bukkit.entity.Mob) {
                        ((org.bukkit.entity.Mob) entity).setTarget(null);

                        // Make entity flee from player
                        org.bukkit.util.Vector fleeVector = entity.getLocation().toVector()
                                .subtract(player.getLocation().toVector())
                                .normalize().multiply(1.5);

                        entity.setVelocity(fleeVector);
                    }
                }

                // Visual and sound effect for battle cry
                player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.8f);

                player.sendMessage(ChatColor.RED + "You unleash a mighty battle cry!");
                return true;

            case "dwarf:stone_skin":
                // Dwarves gain strong resistance
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30 * 20, 2));
                player.sendMessage(ChatColor.GRAY + "Your skin hardens like stone!");
                return true;

            default:
                player.sendMessage(ChatColor.RED + "This ability is not yet implemented.");
                return false;
        }
    }

    public Map<String, Race> getRaces() {
        if (races == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(races);
    }
}


// This class should be in its own file: PlayerRaceChangeEvent.java
class PlayerRaceChangeEvent extends org.bukkit.event.Event {
    private static final org.bukkit.event.HandlerList handlers = new org.bukkit.event.HandlerList();

    private final Player player;
    private final String race;

    public PlayerRaceChangeEvent(Player player, String race) {
        this.player = player;
        this.race = race;
    }

    public Player getPlayer() {
        return player;
    }

    public String getRace() {
        return race;
    }

    @Override
    public org.bukkit.event.HandlerList getHandlers() {
        return handlers;
    }

    public static org.bukkit.event.HandlerList getHandlerList() {
        return handlers;
    }
}