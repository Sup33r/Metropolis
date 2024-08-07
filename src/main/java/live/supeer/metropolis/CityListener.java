package live.supeer.metropolis;

import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import live.supeer.metropolis.city.Claim;
import live.supeer.metropolis.city.Role;
import live.supeer.metropolis.event.*;
import live.supeer.metropolis.utils.LocationUtil;
import live.supeer.metropolis.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.UUID;

public class CityListener implements Listener {
    public static Metropolis plugin;

    @EventHandler
    public void onPlayerEnterCity(PlayerEnterCityEvent event) {
        City city = event.getCity();
        if (city.getEnterMessage() != null) {
            plugin.sendMessage(event.getPlayer(), "messages.city.enter", "%cityname%", city.getCityName(), "%enter%", city.getEnterMessage());
        }
        MetropolisListener.playerInCity.put(event.getPlayer().getUniqueId(), city);
        Utilities.sendCityScoreboard(event.getPlayer(), city, null);

        event.getPlayer().sendMessage("You have entered " + event.getCity().getCityName());
    }

    @EventHandler
    public void onPlayerExitCity(PlayerExitCityEvent event) {
        City city = event.getCity();
        MetropolisListener.playerInCity.remove(event.getPlayer().getUniqueId(), city);
        if (city.getExitMessage() != null) {
            plugin.sendMessage(event.getPlayer(), "messages.city.exit", "%cityname%", city.getCityName(), "%exit%", city.getExitMessage());
        }

        if(event.isToNature()) {
            Utilities.sendNatureScoreboard(event.getPlayer());
        }

        event.getPlayer().sendMessage("You have exited " + event.getCity().getCityName());
    }

    @EventHandler
    public void onPlayerEnterPlot(PlayerEnterPlotEvent event) {
        event.getPlayer().sendMessage("You have entered the plot " + event.getPlot().getPlotName());
        MetropolisListener.playerInPlot.put(event.getPlayer().getUniqueId(), event.getPlot());
        Utilities.sendCityScoreboard(event.getPlayer(), event.getPlot().getCity(), event.getPlot());
    }

    @EventHandler
    public void onPlayerExitPlot(PlayerExitPlotEvent event) {
        event.getPlayer().sendMessage("You have exited the plot " + event.getPlot().getPlotName());
        MetropolisListener.playerInPlot.remove(event.getPlayer().getUniqueId(), event.getPlot());
        Utilities.sendCityScoreboard(event.getPlayer(), event.getPlot().getCity(), null);
    }

    @EventHandler
    public void onAutoclaimAttempt(AutoclaimAttemptEvent event) {
        Player player = event.getPlayer();
        City city = event.getCity();
        Location location = event.getLocation();

        if (isChunkClaimable(city, player, location)) {
            Claim claim = CityDatabase.createClaim(city, location, false, player.getName(), player.getUniqueId().toString());
            if (claim != null) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"buy\", \"subtype\": \"claim\", \"balance\": "
                                + "500"
                                + ", \"claimlocation\": "
                                + LocationUtil.formatChunk(
                                claim.getClaimWorld(), claim.getXPosition(), claim.getZPosition())
                                + ", \"player\": "
                                + player.getUniqueId().toString()
                                + " }");
                CityDatabase.removeCityBalance(city, Metropolis.configuration.getCityClaimCost());
                AutoclaimManager.decrementAutoclaimCount(player);
                PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, city);
                Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
                plugin.sendMessage(player, "messages.city.autoclaim.claimed",
                        "%amount%", String.valueOf(Metropolis.configuration.getCityClaimCost()),
                        "%remaining%", String.valueOf(AutoclaimManager.getAutoclaimInfo(player).getCount()), "%cityname%", city.getCityName());
            }
        } else {
            AutoclaimManager.stopAutoclaim(player);
            plugin.sendMessage(player, "messages.city.autoclaim.unclaimable");
        }
    }

    private boolean isChunkClaimable(City city, Player player, Location location) {
        if (CityDatabase.getCityBalance(city) < Metropolis.configuration.getCityClaimCost()) {
            return false;
        }
        if (CityDatabase.getClaim(location) != null) {
            return false;
        }
        if (Utilities.isCloseToOtherCity(player, location, "city")) {
            return false;
        }
        if (CityDatabase.getCityRole(city, player.getUniqueId().toString()) == null) {
            return false;
        }
        if (!CityDatabase.getCityRole(city, player.getUniqueId().toString()).hasPermission(Role.ASSISTANT)) {
            return false;
        }
        Claim claim1 = CityDatabase.getClaim(location.add(16, 0, 0));
        Claim claim2 = CityDatabase.getClaim(location.add(-16, 0, 0));
        Claim claim3 = CityDatabase.getClaim(location.add(0, 0, 16));
        Claim claim4 = CityDatabase.getClaim(location.add(0, 0, -16));
        return (claim1 != null && claim1.getCity() == city) ||
                (claim2 != null && claim2.getCity() == city) ||
                (claim3 != null && claim3.getCity() == city) ||
                (claim4 != null && claim4.getCity() == city);
    }


}
