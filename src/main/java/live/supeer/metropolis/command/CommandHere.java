package live.supeer.metropolis.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import live.supeer.metropolis.Metropolis;
import org.bukkit.entity.Player;

@CommandAlias("here")
public class CommandHere extends BaseCommand {
    @Default
    @CommandPermission("metropolis.command.here")
    public void onHere(Player player) {
        if (Metropolis.playerInPlot.containsKey(player.getUniqueId())) {
            CommandPlot.sendInfo(player);
            player.sendMessage("\n");
        } else if (Metropolis.playerInCity.containsKey(player.getUniqueId())) {
            CommandCity.sendInfo(player, Metropolis.playerInCity.get(player.getUniqueId()).getCityName());
        } else {
            Metropolis.sendMessage(player, "messages.here.nature");
        }

        if (!player.getWorld().getName().equals("world")) {
            return;
        }
        Metropolis.sendMessage(player, "messages.here.map", "%x%", player.getLocation().getBlockX() + "", "%z%", player.getLocation().getBlockZ() + "");
    }
}
