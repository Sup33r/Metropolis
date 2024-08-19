package live.supeer.metropolis.event;

import live.supeer.metropolis.city.City;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class CityJoinEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final City city;

    public CityJoinEvent(Player player, City city) {
        this.player = player;
        this.city = city;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
