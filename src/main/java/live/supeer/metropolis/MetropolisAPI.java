package live.supeer.metropolis;

import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.plot.PlotDatabase;
import org.bukkit.Location;

public class MetropolisAPI {
    private static Metropolis plugin;

    public MetropolisAPI(Metropolis plugin) {
        MetropolisAPI.plugin = plugin;
    }

    public static City getCityByName(String cityName) {
        return CityDatabase.getCity(cityName).orElse(null);
    }

    public static Plot getPlotByLocation(Location location) {
        return PlotDatabase.getPlotAtLocation(location);
    }
}
