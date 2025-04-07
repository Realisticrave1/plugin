package RavenMC.ravenRPG.listeners;

import RavenMC.ravenRPG.RavenRPG;
import RavenMC.ravenRPG.Managers.PlayerData;
import RavenMC.ravenRPG.Managers.PlayerRaven;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RavenAbilityListener implements Listener {

    private final RavenRPG plugin;
    private final Map<UUID, Long> lastEffectTime = new HashMap<>();
    private static final long EFFECT_COOLDOWN = 1000; // 1 second in milliseconds

    public RavenAbilityListener(RavenRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Don't process if the player hasn't actually moved blocks
        if (event.getFrom().getBlock().equals(event.getTo().getBlock())) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check if player has a raven
        PlayerRaven raven = plugin.getRavenManager().getRaven(playerId);
        if (raven == null) {
            return;
        }

        // Only process visual effects every second to avoid spam
        if (!canApplyEffect(playerId)) {
            return;
        }

        // Apply passive effects based on raven type and location
        String ravenType = raven.getType();
        int ravenLevel = raven.getLevel();

        switch (ravenType.toLowerCase()) {
            case "scout":
                // Scout ravens help detect resources and dangers
                if (ravenLevel >= 5) {
                    detectNearbyResources(player, ravenLevel);
                }
                break;

            case "guardian":
                // Guardian ravens occasionally warn of dangers
                if (ravenLevel >= 5) {
                    detectNearbyDangers(player, ravenLevel);
                }
                break;

            case "hunter":
                // Hunter ravens help detect animals
                if (ravenLevel >= 5) {
                    detectNearbyAnimals(player, ravenLevel);
                }
                break;

            case "arcane":
                // Arcane ravens enhance magic and regenerate mana
                if (ravenLevel >= 5) {
                    enhanceMagic(player, ravenLevel);
                }
                break;
        }

        // Visual effects to show the raven's presence
        displayRavenEffects(player, ravenType, raven.getColor());

        // Update last effect time
        lastEffectTime.put(playerId, System.currentTimeMillis());
    }

    private boolean canApplyEffect(UUID playerId) {
        if (!lastEffectTime.containsKey(playerId)) {
            return true;
        }

        long lastTime = lastEffectTime.get(playerId);
        return System.currentTimeMillis() - lastTime >= EFFECT_COOLDOWN;
    }

    private void detectNearbyResources(Player player, int ravenLevel) {
        int radius = 5 + (ravenLevel / 2); // Scales with level

        for (Block block : getNearbyBlocks(player.getLocation(), radius)) {
            if (isValuableResource(block.getType())) {
                // Visual indicator for valuable blocks
                player.spawnParticle(Particle.VILLAGER_HAPPY,
                        block.getLocation().add(0.5, 0.5, 0.5),
                        3, 0.2, 0.2, 0.2, 0);

                // Only notify for very valuable resources (to avoid spam)
                if (isHighValueResource(block.getType()) && Math.random() < 0.1) { // 10% chance
                    player.sendMessage(ChatColor.AQUA + "Your scout raven senses valuable resources nearby!");
                }
            }
        }
    }

    private void detectNearbyDangers(Player player, int ravenLevel) {
        int radius = 10 + ravenLevel; // Scales with level
        boolean foundDanger = false;

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (isDangerous(entity)) {
                // Visual indicator for dangerous entities
                player.spawnParticle(Particle.SMOKE_NORMAL,
                        entity.getLocation().add(0, 1, 0),
                        5, 0.2, 0.5, 0.2, 0);

                foundDanger = true;
            }
        }

        // Notify player of danger, but not too often
        if (foundDanger && Math.random() < 0.2) { // 20% chance
            player.sendMessage(ChatColor.RED + "Your guardian raven senses danger nearby!");
        }
    }

    private void detectNearbyAnimals(Player player, int ravenLevel) {
        int radius = 15 + ravenLevel; // Scales with level
        boolean foundAnimal = false;

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (isHuntable(entity)) {
                // Visual indicator for huntable entities
                player.spawnParticle(Particle.HEART,
                        entity.getLocation().add(0, 1, 0),
                        3, 0.2, 0.2, 0.2, 0);

                foundAnimal = true;
            }
        }

        // Notify player of animals, but not too often
        if (foundAnimal && Math.random() < 0.1) { // 10% chance
            player.sendMessage(ChatColor.GREEN + "Your hunter raven spots prey nearby!");
        }
    }

    private void enhanceMagic(Player player, int ravenLevel) {
        // Regenerate mana faster
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        int manaBonus = ravenLevel / 5; // 1 mana per 5 levels

        if (manaBonus > 0 && playerData.getMana() < playerData.getMaxMana() && Math.random() < 0.2) {
            int newMana = Math.min(playerData.getMana() + manaBonus, playerData.getMaxMana());
            playerData.setMana(newMana);

            // Visual effect for mana regeneration
            player.spawnParticle(Particle.SPELL_WITCH,
                    player.getLocation().add(0, 1, 0),
                    10, 0.5, 0.5, 0.5, 0.05);

            // Notify player occasionally
            if (Math.random() < 0.1) { // 10% chance
                player.sendMessage(ChatColor.LIGHT_PURPLE + "Your arcane raven channels magical energy to you.");
            }
        }
    }

    private void displayRavenEffects(Player player, String ravenType, int ravenColor) {
        // Create particle effects based on raven type
        Particle particle;

        switch (ravenType.toLowerCase()) {
            case "scout":
                particle = Particle.END_ROD;
                break;
            case "guardian":
                particle = Particle.FLAME;
                break;
            case "hunter":
                particle = Particle.CRIT;
                break;
            case "arcane":
                particle = Particle.SPELL_WITCH;
                break;
            default:
                particle = Particle.CLOUD;
                break;
        }

        // Calculate color components for colored particles
        float red = ((ravenColor >> 16) & 0xFF) / 255.0F;
        float green = ((ravenColor >> 8) & 0xFF) / 255.0F;
        float blue = (ravenColor & 0xFF) / 255.0F;

        // Spawn particles around the player (as if the raven is circling)
        double angle = (System.currentTimeMillis() % 2000) / 2000.0 * 2 * Math.PI;
        double radius = 1.2;
        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;
        double y = 1.5 + Math.sin(angle * 2) * 0.2; // Slight up and down movement

        Location particleLoc = player.getLocation().add(x, y, z);

        // Colored dust particles for personalized ravens
        player.spawnParticle(Particle.REDSTONE,
                particleLoc,
                3, 0.1, 0.1, 0.1, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(
                        (int)(red * 255), (int)(green * 255), (int)(blue * 255)), 1.0F));

        // Type-specific particle effect
        player.spawnParticle(particle,
                particleLoc,
                1, 0.05, 0.05, 0.05, 0.01);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageEvent event) {
        // Handle passive protection from guardian ravens
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        UUID playerId = player.getUniqueId();

        // Check if player has a guardian raven
        PlayerRaven raven = plugin.getRavenManager().getRaven(playerId);
        if (raven == null || !raven.getType().equalsIgnoreCase("guardian")) {
            return;
        }

        // Guardian ravens have a chance to reduce damage
        int ravenLevel = raven.getLevel();
        double damageReductionChance = 0.05 + (ravenLevel * 0.01); // 5% base + 1% per level

        if (Math.random() < damageReductionChance) {
            // Reduce damage
            double reduction = 0.2 + (ravenLevel * 0.01); // 20% base + 1% per level
            double newDamage = event.getDamage() * (1 - reduction);
            event.setDamage(newDamage);

            // Visual and sound effect
            player.spawnParticle(Particle.BLOCK_DUST,
                    player.getLocation().add(0, 1, 0),
                    10, 0.5, 0.5, 0.5, 0.2);
            player.getWorld().playSound(player.getLocation(),
                    org.bukkit.Sound.ENTITY_ENDER_DRAGON_FLAP,
                    0.5f, 1.2f);

            player.sendMessage(ChatColor.GOLD + "Your guardian raven protected you from harm!");
        }
    }

    // Helper methods

    private java.util.List<Block> getNearbyBlocks(Location location, int radius) {
        java.util.List<Block> blocks = new java.util.ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    // Skip blocks that are too far away
                    if (x*x + y*y + z*z > radius*radius) {
                        continue;
                    }

                    Block block = location.getBlock().getRelative(x, y, z);
                    blocks.add(block);
                }
            }
        }

        return blocks;
    }

    private boolean isValuableResource(Material material) {
        return material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE ||
                material == Material.EMERALD_ORE || material == Material.DEEPSLATE_EMERALD_ORE ||
                material == Material.GOLD_ORE || material == Material.DEEPSLATE_GOLD_ORE ||
                material == Material.IRON_ORE || material == Material.DEEPSLATE_IRON_ORE ||
                material == Material.LAPIS_ORE || material == Material.DEEPSLATE_LAPIS_ORE ||
                material == Material.REDSTONE_ORE || material == Material.DEEPSLATE_REDSTONE_ORE ||
                material == Material.COPPER_ORE || material == Material.DEEPSLATE_COPPER_ORE ||
                material == Material.ANCIENT_DEBRIS || material == Material.NETHER_GOLD_ORE ||
                material == Material.NETHER_QUARTZ_ORE;
    }

    private boolean isHighValueResource(Material material) {
        return material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE ||
                material == Material.EMERALD_ORE || material == Material.DEEPSLATE_EMERALD_ORE ||
                material == Material.ANCIENT_DEBRIS;
    }

    private boolean isDangerous(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }

        // Consider most monsters dangerous
        return entity.getType() == EntityType.ZOMBIE || entity.getType() == EntityType.SKELETON ||
                entity.getType() == EntityType.CREEPER || entity.getType() == EntityType.SPIDER ||
                entity.getType() == EntityType.CAVE_SPIDER || entity.getType() == EntityType.ENDERMAN ||
                entity.getType() == EntityType.WITCH || entity.getType() == EntityType.SLIME ||
                entity.getType() == EntityType.PHANTOM || entity.getType() == EntityType.DROWNED ||
                entity.getType() == EntityType.BLAZE || entity.getType() == EntityType.GHAST ||
                entity.getType() == EntityType.WITHER_SKELETON || entity.getType() == EntityType.MAGMA_CUBE ||
                entity.getType() == EntityType.RAVAGER || entity.getType() == EntityType.VEX ||
                entity.getType() == EntityType.VINDICATOR || entity.getType() == EntityType.PILLAGER ||
                entity.getType() == EntityType.EVOKER || entity.getType() == EntityType.ELDER_GUARDIAN ||
                entity.getType() == EntityType.SHULKER || entity.getType() == EntityType.PIGLIN_BRUTE ||
                entity.getType() == EntityType.WARDEN;
    }

    private boolean isHuntable(Entity entity) {
        return entity.getType() == EntityType.COW || entity.getType() == EntityType.PIG ||
                entity.getType() == EntityType.SHEEP || entity.getType() == EntityType.CHICKEN ||
                entity.getType() == EntityType.RABBIT || entity.getType() == EntityType.FOX ||
                entity.getType() == EntityType.WOLF || entity.getType() == EntityType.POLAR_BEAR ||
                entity.getType() == EntityType.TURTLE || entity.getType() == EntityType.TROPICAL_FISH ||
                entity.getType() == EntityType.SALMON || entity.getType() == EntityType.COD ||
                entity.getType() == EntityType.PUFFERFISH || entity.getType() == EntityType.SQUID ||
                entity.getType() == EntityType.DOLPHIN;
    }
}