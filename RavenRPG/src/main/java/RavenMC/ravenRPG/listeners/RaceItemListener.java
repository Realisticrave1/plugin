package RavenMC.ravenRPG.listeners;

import RavenMC.ravenRPG.RavenRPG;
import RavenMC.ravenRPG.Managers.RaceItemManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ListIterator;

public class RaceItemListener implements Listener {

    private final RavenRPG plugin;
    private final RaceItemManager raceItemManager;

    public RaceItemListener(RavenRPG plugin) {
        this.plugin = plugin;
        this.raceItemManager = plugin.getRaceItemManager();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Give ability items after a short delay to ensure data is loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                raceItemManager.setupPlayerAbilityItems(player);
            }
        }.runTaskLater(plugin, 20L); // 1 second delay
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Give ability items after respawn
        new BukkitRunnable() {
            @Override
            public void run() {
                raceItemManager.setupPlayerAbilityItems(player);
            }
        }.runTaskLater(plugin, 20L); // 1 second delay
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Prevent ability items from dropping on death
        ListIterator<ItemStack> iter = event.getDrops().listIterator();
        while (iter.hasNext()) {
            ItemStack item = iter.next();
            if (raceItemManager.isAbilityItem(item)) {
                iter.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        // Prevent moving ability items
        if (!(event.getWhoClicked() instanceof Player)) return;

        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && raceItemManager.isAbilityItem(currentItem)) {
            event.setCancelled(true);
            ((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "You cannot move race ability items!");
            return;
        }

        // Prevent placing items in ability slots
        if (event.getSlot() == RaceItemManager.PRIMARY_ABILITY_SLOT ||
                event.getSlot() == RaceItemManager.SECONDARY_ABILITY_SLOT) {
            if (event.getClickedInventory() != null &&
                    event.getClickedInventory().equals(event.getWhoClicked().getInventory())) {
                event.setCancelled(true);
                ((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "These slots are reserved for race abilities!");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        // Prevent dragging onto ability slots
        if (!(event.getWhoClicked() instanceof Player)) return;

        for (int slot : event.getRawSlots()) {
            if (slot == RaceItemManager.PRIMARY_ABILITY_SLOT ||
                    slot == RaceItemManager.SECONDARY_ABILITY_SLOT) {
                event.setCancelled(true);
                ((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "These slots are reserved for race abilities!");
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        // Prevent dropping ability items
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        if (raceItemManager.isAbilityItem(droppedItem)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot drop race ability items!");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Handle right-click ability activation
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if this is a right-click with an ability item
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (item != null && raceItemManager.isAbilityItem(item)) {
                // Cancel the event to prevent normal item use
                event.setCancelled(true);

                // Get ability ID and activate it
                String abilityId = raceItemManager.getAbilityId(item);
                if (abilityId != null) {
                    plugin.getRaceManager().activateRaceAbility(player, abilityId);

                    // Update cooldown display after a short delay (to allow cooldown to be set)
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            int remainingCooldown = plugin.getRaceManager().getRemainingCooldown(player.getUniqueId(), abilityId);
                            raceItemManager.updateCooldownDisplay(player, abilityId, remainingCooldown);
                        }
                    }.runTaskLater(plugin, 5L);
                }
            }
        }
    }
}