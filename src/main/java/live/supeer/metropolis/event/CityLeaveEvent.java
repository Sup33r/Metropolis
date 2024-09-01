package live.supeer.metropolis.event;

import live.supeer.apied.MPlayer;
import live.supeer.metropolis.city.City;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class CityLeaveEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final MPlayer mPlayer;
    private final City city;

    public CityLeaveEvent(MPlayer mPlayer, City city) {
        this.mPlayer = mPlayer;
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
