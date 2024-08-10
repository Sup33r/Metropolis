package live.supeer.metropolis.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import org.bukkit.entity.Player;

@CommandAlias("metropolis | mp")
public class CommandMetropolis extends BaseCommand {
    public static Metropolis plugin;


    @Subcommand("city")
    public class City extends BaseCommand {


        @Subcommand("set")
        public class Set extends BaseCommand {

            @Subcommand("minchunkdistance")
            public void onSetMinChunkDistance(Player player, String distance, String cityName) {
                if (player.hasPermission("metropolis.admin.city.set.minchunkdistance")) {
                    live.supeer.metropolis.city.City city = CityDatabase.getCity(cityName).get();
                    if (CityDatabase.getCity(cityName).isEmpty()) {
                        plugin.sendMessage(player, "messages.error.city.notFound");
                        return;
                    }
                    if (distance.equals("-")) {
                        city.setMinChunkDistance(Metropolis.configuration.getMinChunkDistance());
                        plugin.sendMessage(player, "messages.city.successful.resetMinChunkDistance","%cityname%", city.getCityName());
                        return;
                    }
                    if (distance.matches("[0-9]+")) {
                        city.setMinChunkDistance(Integer.parseInt(distance));
                        plugin.sendMessage(player, "messages.city.successful.setMinChunkDistance", "%distance%", distance, "%cityname%", city.getCityName());
                        return;
                    }
                    plugin.sendMessage(player, "messages.error.invalidNumber");
                } else {
                    plugin.sendMessage(player, "messages.error.permissionDenied");
                }
            }

            @Subcommand("minspawndistance")
            public void onSetMinSpawnDistance(Player player, String distance, String cityName) {
                if (player.hasPermission("metropolis.admin.city.set.minspawndistance")) {
                    live.supeer.metropolis.city.City city = CityDatabase.getCity(cityName).get();
                    if (CityDatabase.getCity(cityName).isEmpty()) {
                        plugin.sendMessage(player, "messages.error.city.notFound");
                        return;
                    }
                    if (distance.equals("-")) {
                        city.setMinSpawnDistance(Metropolis.configuration.getMinSpawnDistance());
                        plugin.sendMessage(player, "messages.city.successful.resetMinSpawnDistance","%cityname%", city.getCityName());
                        return;
                    }
                    if (distance.matches("[0-9]+")) {
                        city.setMinSpawnDistance(Integer.parseInt(distance));
                        plugin.sendMessage(player, "messages.city.successful.setMinSpawnDistance", "%distance%", distance, "%cityname%", city.getCityName());
                        return;
                    }
                    plugin.sendMessage(player, "messages.error.invalidNumber");
                } else {
                    plugin.sendMessage(player, "messages.error.permissionDenied");
                }
            }
        }
    }
}
