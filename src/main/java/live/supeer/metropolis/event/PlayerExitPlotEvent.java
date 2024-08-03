package live.supeer.metropolis.event;

import live.supeer.metropolis.plot.Plot;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerExitPlotEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Plot plot;

    public PlayerExitPlotEvent(Player player, Plot plot) {
        this.player = player;
        this.plot = plot;
    }

    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
