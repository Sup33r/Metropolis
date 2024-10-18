package live.supeer.metropolis;

import live.supeer.metropolis.city.*;
import live.supeer.metropolis.event.*;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.utils.LocationUtil;
import live.supeer.metropolis.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Objects;

public class CityListener implements Listener {

    @EventHandler
    public void onPlayerEnterCity(PlayerEnterCityEvent event) {
        City city = event.getCity();
        if (city.getEnterMessage() != null) {
            if (city.isReserve()) {
                Metropolis.sendMessage(event.getPlayer(), "messages.city.enter.reserve", "%cityname%", city.getCityName(), "%enter%", city.getEnterMessage());
            } else {
                Metropolis.sendMessage(event.getPlayer(), "messages.city.enter.normal", "%cityname%", city.getCityName(), "%enter%", city.getExitMessage());
            }
        }
        Metropolis.playerInCity.put(event.getPlayer().getUniqueId(), city);
        Utilities.sendCityScoreboard(event.getPlayer(), city, null);
    }

    @EventHandler
    public void onPlayerExitCity(PlayerExitCityEvent event) {
        City city = event.getCity();
        Metropolis.playerInCity.remove(event.getPlayer().getUniqueId(), city);
        Metropolis.playerInDistrict.remove(event.getPlayer().getUniqueId());
        Metropolis.playerInPlot.remove(event.getPlayer().getUniqueId());

        if (city.getExitMessage() != null) {
            if (city.isReserve()) {
                Metropolis.sendMessage(event.getPlayer(), "messages.city.exit.reserve", "%cityname%", city.getCityName(), "%exit%", city.getExitMessage());
            } else {
                Metropolis.sendMessage(event.getPlayer(), "messages.city.exit.normal", "%cityname%", city.getCityName(), "%exit%", city.getExitMessage());
            }
        }

        if(event.isToNature()) {
            Utilities.sendNatureScoreboard(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerEnterPlot(PlayerEnterPlotEvent event) {
        Metropolis.playerInPlot.put(event.getPlayer().getUniqueId(), event.getPlot());
        Utilities.sendCityScoreboard(event.getPlayer(), event.getPlot().getCity(), event.getPlot());
    }

    @EventHandler
    public void onPlayerExitPlot(PlayerExitPlotEvent event) {
        Metropolis.playerInPlot.remove(event.getPlayer().getUniqueId(), event.getPlot());
        Utilities.sendCityScoreboard(event.getPlayer(), event.getPlot().getCity(), null);
    }

    @EventHandler
    public void onPlayerEnterDistrict(PlayerEnterDistrictEvent event) {
        Metropolis.playerInDistrict.put(event.getPlayer().getUniqueId(), event.getDistrict());
        Plot plot = Metropolis.playerInPlot.get(event.getPlayer().getUniqueId());
        Utilities.sendCityScoreboard(event.getPlayer(), event.getDistrict().getCity(), plot);
    }

    @EventHandler
    public void onPlayerExitDistrict(PlayerExitDistrictEvent event) {
        Metropolis.playerInDistrict.remove(event.getPlayer().getUniqueId(), event.getDistrict());
        Plot plot = Metropolis.playerInPlot.get(event.getPlayer().getUniqueId());
        Utilities.sendCityScoreboard(event.getPlayer(), event.getDistrict().getCity(), plot);
    }

    @EventHandler
    public void onCityLeaveEvent(CityLeaveEvent event) {
        for (Plot plot : event.getCity().getCityPlots()) {
            if (Objects.equals(plot.getPlotOwnerUUID(), event.getMPlayer().getUuid().toString())) {
                plot.removePlotOwner();
            }
        }
        for (District district : event.getCity().getCityDistricts()) {
            if (district.getContactplayers().contains(event.getMPlayer().getUuid())) {
                district.removeContactPlayer(event.getMPlayer());
            }
        }
    }

    @EventHandler
    public void onAutoclaimAttempt(AutoclaimAttemptEvent event) {
        Player player = event.getPlayer();
        City city = event.getCity();
        Location location = event.getLocation();

        if (isChunkClaimable(city, player, location)) {
            Claim claim = CityDatabase.createClaim(city, location, false, player.getUniqueId().toString());
            if (claim != null) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"buy\", \"subtype\": \"claim\", \"balance\": "
                                + "500"
                                + ", \"claimlocation\": "
                                + LocationUtil.formatChunk(
                                claim.getClaimWorld().getName(), claim.getXPosition(), claim.getZPosition())
                                + ", \"player\": "
                                + player.getUniqueId()
                                + " }");
                CityDatabase.removeCityBalance(city, Metropolis.configuration.getCityClaimCost());
                AutoclaimManager.decrementAutoclaimCount(player);
                PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, city);
                Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
                Metropolis.sendMessage(player, "messages.city.autoclaim.claimed",
                        "%amount%", String.valueOf(Metropolis.configuration.getCityClaimCost()),
                        "%remaining%", String.valueOf(AutoclaimManager.getAutoclaimInfo(player).getCount()), "%cityname%", city.getCityName());
            }
        } else {
            AutoclaimManager.stopAutoclaim(player);
            Metropolis.sendMessage(player, "messages.city.autoclaim.unclaimable");
        }
    }

    private boolean isChunkClaimable(City city, Player player, Location location) {
        if (CityDatabase.getCityBalance(city) < Metropolis.configuration.getCityClaimCost()) {
            return false;
        }
        if (CityDatabase.getClaim(location) != null) {
            return false;
        }
        if (Utilities.cannotClaimOrCreateCity(player.getLocation().toBlockLocation(), city)) {
            return false;
        }
        if (CityDatabase.getCityRole(city, player.getUniqueId().toString()) == null) {
            return false;
        }
        if (!Utilities.cityCanClaim(city)) {
            Metropolis.sendMessage(player, "messages.error.city.maxClaims", "%cityname%", city.getCityName());
            return false;
        }
        if (!Objects.requireNonNull(CityDatabase.getCityRole(city, player.getUniqueId().toString())).hasPermission(Role.ASSISTANT)) {
            return false;
        }
        Claim claim1 = CityDatabase.getClaim(location.toBlockLocation().add(16, 0, 0));
        Claim claim2 = CityDatabase.getClaim(location.toBlockLocation().add(-16, 0, 0));
        Claim claim3 = CityDatabase.getClaim(location.toBlockLocation().add(0, 0, 16));
        Claim claim4 = CityDatabase.getClaim(location.toBlockLocation().add(0, 0, -16));
        return (claim1 != null && claim1.getCity() == city) ||
                (claim2 != null && claim2.getCity() == city) ||
                (claim3 != null && claim3.getCity() == city) ||
                (claim4 != null && claim4.getCity() == city);
    }
}
