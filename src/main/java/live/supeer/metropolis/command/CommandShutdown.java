package live.supeer.metropolis.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.Standing;
import live.supeer.metropolis.plot.PlotDatabase;
import live.supeer.metropolis.utils.DateUtil;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Objects;

@CommandAlias("shutdown|stop")
public class CommandShutdown extends BaseCommand {

    @Default
    @CommandPermission("metropolis.shutdown")
    public void onCommand(@Default("15s") String time) {
        long futureTime = DateUtil.parseDateDiff(time, true);
        long length = futureTime - System.currentTimeMillis();

        Bukkit.getScheduler().runTaskLater(Metropolis.getInstance(), () -> {
            for (List<Standing> standings : Metropolis.plotStandings.values()) {
                PlotDatabase.storeStandings(standings);
            }
            Metropolis.getInstance().getServer().broadcast(Objects.requireNonNull(Metropolis.getMessageComponent("messages.shutdownMessage")));
            Metropolis.getInstance().logger.info("Saved standings...");
            Metropolis.getInstance().getServer().shutdown();
        }, length / 50);
        Metropolis.getInstance().getServer().broadcast(Objects.requireNonNull(Metropolis.getMessageComponent("messages.shutdownMessageSoon")));
    }
}
