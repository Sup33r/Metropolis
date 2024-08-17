package live.supeer.metropolis.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import live.supeer.metropolis.Database;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.city.CityDatabase;
import live.supeer.metropolis.utils.Utilities;
import org.bukkit.entity.Player;

@CommandAlias("metropolis | mp")
public class CommandMetropolis extends BaseCommand {
    @Subcommand("taxcollect")
    public void onTaxCollect(Player player) {
        if (player.hasPermission("metropolis.admin.taxcollect")) {
            CityDatabase.collectTaxes();
        } else {
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
        }
    }


    @Subcommand("city")
    public class City extends BaseCommand {


        @Subcommand("balance")
        @CommandCompletion("@nothing @cityNames")
        public void onCityBalance(Player player, String argument, String cityName) {
            if (player.hasPermission("metropolis.admin.city.balance")) {
                live.supeer.metropolis.city.City city = CityDatabase.getCity(cityName).get();
                if (CityDatabase.getCity(cityName).isEmpty()) {
                    Metropolis.sendMessage(player, "messages.error.city.notFound");
                    return;
                }
                if (argument == null) {
                    Metropolis.sendMessage(player,"messages.syntax.admin.balance");
                    return;
                }
                if (argument.startsWith("+")) {
                    if (argument.substring(1).replaceAll("[0-9]", "").matches("[^0-9]")
                            || argument.length() == 1) {
                        Metropolis.sendMessage(player, "messages.syntax.admin.balance");
                        return;
                    }
                    int inputBalance = Integer.parseInt(argument.replaceAll("[^0-9]", ""));
                    CityDatabase.addCityBalance(city, inputBalance);
                    Database.addLogEntry(
                            city,
                            "{ \"type\": \"cityBank\", \"subtype\": \"deposit\", \"balance\": "
                                    + inputBalance
                                    + ", \"player\": "
                                    + player.getUniqueId().toString()
                                    + " }");
                    Metropolis.sendMessage(
                            player,
                            "messages.city.successful.deposit",
                            "%amount%",
                            Utilities.formattedMoney(inputBalance),
                            "%cityname%",
                            cityName);
                    return;
                }
                if (argument.startsWith("-")) {
                    if (argument.substring(1).replaceAll("[0-9]", "").matches("[^0-9]")
                            || argument.length() == 1) {
                        Metropolis.sendMessage(player, "messages.syntax.admin.balance");
                        return;
                    }
                    int inputBalance = Integer.parseInt(argument.replaceAll("[^0-9]", ""));
                    String inputBalanceFormatted = Utilities.formattedMoney(inputBalance);
                    int cityBalance = city.getCityBalance();
                    CityDatabase.removeCityBalance(city, inputBalance);
                    Database.addLogEntry(
                            city,
                            "{ \"type\": \"cityBank\", \"subtype\": \"withdraw\", \"balance\": "
                                    + inputBalance
                                    + ", \"player\": "
                                    + player.getUniqueId().toString()
                                    + "\" }");
                    Metropolis.sendMessage(
                            player,
                            "messages.city.successful.withdraw",
                            "%amount%",
                            inputBalanceFormatted,
                            "%cityname%",
                            city.getCityName());
                    return;
                }
                Metropolis.sendMessage(player, "messages.syntax.admin.balance");
            } else {
                Metropolis.sendMessage(player, "messages.error.permissionDenied");
            }
        }


        @Subcommand("set")
        public class Set extends BaseCommand {

            @Subcommand("minchunkdistance")
            @CommandCompletion("@nothing @cityNames")
            public void onSetMinChunkDistance(Player player, String distance, String cityName) {
                if (player.hasPermission("metropolis.admin.city.set.minchunkdistance")) {
                    live.supeer.metropolis.city.City city = CityDatabase.getCity(cityName).get();
                    if (CityDatabase.getCity(cityName).isEmpty()) {
                        Metropolis.sendMessage(player, "messages.error.city.notFound");
                        return;
                    }
                    if (distance.equals("-")) {
                        city.setMinChunkDistance(Metropolis.configuration.getMinChunkDistance());
                        Metropolis.sendMessage(player, "messages.city.successful.resetMinChunkDistance","%cityname%", city.getCityName());
                        return;
                    }
                    if (distance.matches("[0-9]+")) {
                        city.setMinChunkDistance(Integer.parseInt(distance));
                        Metropolis.sendMessage(player, "messages.city.successful.setMinChunkDistance", "%distance%", distance, "%cityname%", city.getCityName());
                        return;
                    }
                    Metropolis.sendMessage(player, "messages.error.invalidNumber");
                } else {
                    Metropolis.sendMessage(player, "messages.error.permissionDenied");
                }
            }

            @Subcommand("minspawndistance")
            @CommandCompletion("@nothing @cityNames")
            public void onSetMinSpawnDistance(Player player, String distance, String cityName) {
                if (player.hasPermission("metropolis.admin.city.set.minspawndistance")) {
                    live.supeer.metropolis.city.City city = CityDatabase.getCity(cityName).get();
                    if (CityDatabase.getCity(cityName).isEmpty()) {
                        Metropolis.sendMessage(player, "messages.error.city.notFound");
                        return;
                    }
                    if (distance.equals("-")) {
                        city.setMinSpawnDistance(Metropolis.configuration.getMinSpawnDistance());
                        Metropolis.sendMessage(player, "messages.city.successful.resetMinSpawnDistance","%cityname%", city.getCityName());
                        return;
                    }
                    if (distance.matches("[0-9]+")) {
                        city.setMinSpawnDistance(Integer.parseInt(distance));
                        Metropolis.sendMessage(player, "messages.city.successful.setMinSpawnDistance", "%distance%", distance, "%cityname%", city.getCityName());
                        return;
                    }
                    Metropolis.sendMessage(player, "messages.error.invalidNumber");
                } else {
                    Metropolis.sendMessage(player, "messages.error.permissionDenied");
                }
            }
        }
    }
}
