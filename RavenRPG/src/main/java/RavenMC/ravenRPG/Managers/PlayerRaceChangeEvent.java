package RavenMC.ravenRPG.Managers;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event that is fired when a player changes their race
 */
public class PlayerRaceChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String race;

    /**
     * Creates a new PlayerRaceChangeEvent
     *
     * @param player The player who changed race
     * @param race The new race the player has changed to
     */
    public PlayerRaceChangeEvent(Player player, String race) {
        this.player = player;
        this.race = race;
    }

    /**
     * Gets the player who changed race
     *
     * @return The player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the new race of the player
     *
     * @return The race ID
     */
    public String getRace() {
        return race;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * Gets the handler list for this event.
     * This is required by Bukkit's event system.
     *
     * @return The handler list
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }
}