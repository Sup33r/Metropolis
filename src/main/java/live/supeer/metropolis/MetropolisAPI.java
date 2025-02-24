package live.supeer.metropolis;

import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import live.supeer.metropolis.city.Member;
import live.supeer.metropolis.city.Role;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.utils.Utilities;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class MetropolisAPI {
    @Setter
    private static Metropolis plugin;

    public static City getCityByName(String cityName) {
        return CityDatabase.getCity(cityName).orElse(null);
    }

    public static List<Member> getCityMembers(City city) { return city.getCityMembers(); }

    public static boolean isPlayerCityMember(Player player, String cityName) {
        City city = getCityByName(cityName);
        if (city == null) {
            return false;
        }
        return city.isMember(player.getUniqueId());
    }

    public static boolean isInCity(Location location) {
        return CityDatabase.getCityByClaim(location) != null;
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

    public static boolean playerHasCityPermission(Location location, Player player, Role target) {
        City city = CityDatabase.getCityByClaim(location);
        if (city == null) {
            return false;
        }
        Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
        if (role == null) {
            return false;
        }
        return role.permissionLevel() >= role.permissionLevel();
    }

    public static boolean playerHasLocationPermissionFlags(UUID uuid, Location location, char flag) {
        return Utilities.hasLocationPermissionFlags(uuid, location, flag);
    }
}
