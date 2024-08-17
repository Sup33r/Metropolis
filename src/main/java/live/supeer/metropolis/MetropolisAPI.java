package live.supeer.metropolis;

import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import live.supeer.metropolis.city.Member;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.plot.PlotDatabase;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class MetropolisAPI {
    @Setter
    private static Metropolis plugin;

    public static City getCityByName(String cityName) {
        return CityDatabase.getCity(cityName).orElse(null);
    }

    public static Plot getPlotByLocation(Location location) {
        return PlotDatabase.getPlotAtLocation(location);
    }

    public static List<Member> getCityMembers(City city) { return city.getCityMembers(); }

    public static boolean isPlayerCityMember(Player player, String cityName) {
        City city = getCityByName(cityName);
        if (city == null) {
            return false;
        }
        return city.isMember(player.getUniqueId());
    }

    public static City getHomeCity(Player player) {
        return HCDatabase.getHomeCityToCity(player.getUniqueId().toString());
    }

    public static boolean isPlayerInMeeting(Player player) {
        if (!Metropolis.playerInPlot.containsKey(player.getUniqueId())) {
            return false;
        }
        Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
        if (plot == null) {
            return false;
        }
        return plot.hasFlag('c');
    }

    public static List<Player> playersInPlotAtPlayer(Player player) {
        if (!Metropolis.playerInPlot.containsKey(player.getUniqueId())) {
            return null;
        }
        Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
        if (plot == null) {
            return null;
        }
        return plot.playersInPlot();
    }

    public static List<Player> playersInPlot(Plot plot) {
        return plot.playersInPlot();
    }
}
