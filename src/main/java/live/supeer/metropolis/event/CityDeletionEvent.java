package live.supeer.metropolis.event;

import live.supeer.metropolis.city.City;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class CityDeletionEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final City city;

    public CityDeletionEvent(City city) {
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
