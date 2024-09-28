package live.supeer.metropolis.utils;

import live.supeer.apied.ApiedAPI;
import live.supeer.metropolis.Metropolis;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TeleportUtil {
    public static List<Player> teleportingPlayers = new ArrayList<>();
    

    public static void teleport(Player player, Location location, String name, boolean bypassCooldown) {
        if (player.isInsideVehicle()) {
            Metropolis.sendMessage(player, "messages.error.teleport.failed");
            return;
        }
        if (player.hasPermission("mandatory.teleport.nowait") || bypassCooldown) {
            Metropolis.backLocations.put(player, player.getLocation());
            player.teleport(location);
            sendAfkMessages(player);
            Metropolis.sendMessage(player, "messages.teleport.success","%name%", name);
            return;
        }
        if (teleportingPlayers.contains(player)) {
            Metropolis.sendMessage(player, "messages.teleport.hasTeleport");
            return;
        }
        teleportingPlayers.add(player);
        int cooldown = Metropolis.configuration.getTeleportCooldown();
        Metropolis.sendMessage(player, "messages.teleport.started", "%cooldown%", String.valueOf(cooldown));
        Metropolis.getInstance().getServer().getScheduler().runTaskLater(Metropolis.getInstance(), () -> {
            if (!teleportingPlayers.contains(player)) {
                sendAfkMessages(player);
                Metropolis.sendMessage(player, "messages.teleport.failed");
                return;
            }
            sendAfkMessages(player);
            Metropolis.backLocations.put(player, player.getLocation());
            player.teleport(location);
            Metropolis.sendMessage(player, "messages.teleport.success", "%name%", name);
            teleportingPlayers.remove(player);
        }, 20L * cooldown);
    }

    public static void sendAfkMessages(Player player) {
        if (Metropolis.afkPlayers.containsKey(player.getUniqueId())) {
            List<Component> messages = Metropolis.afkPlayers.get(player.getUniqueId());
            for (Component message : messages) {
                player.sendMessage(message);
            }
            Objects.requireNonNull(player.getPlayer()).playerListName(ApiedAPI.getTABPrefix(player.getPlayer(), false));
            Metropolis.afkPlayers.remove(player.getUniqueId());
        }
    }
}
