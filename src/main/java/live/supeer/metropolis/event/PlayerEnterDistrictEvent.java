package live.supeer.metropolis.event;

import live.supeer.metropolis.city.District;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerEnterDistrictEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final District district;

    public PlayerEnterDistrictEvent(Player player, District district) {
        this.player = player;
        this.district = district;
    }
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
