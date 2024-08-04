package live.supeer.metropolis;

import live.supeer.metropolis.city.City;
import live.supeer.metropolis.event.PlayerEnterCityEvent;
import live.supeer.metropolis.event.PlayerEnterPlotEvent;
import live.supeer.metropolis.event.PlayerExitCityEvent;
import live.supeer.metropolis.event.PlayerExitPlotEvent;
import live.supeer.metropolis.utils.Utilities;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CityListener implements Listener {
    public static Metropolis plugin;


    @EventHandler
    public void onPlayerEnterCity(PlayerEnterCityEvent event) {
        City city = event.getCity();
        if (city.getEnterMessage() != null) {
            plugin.sendMessage(event.getPlayer(), "messages.city.enter", "%cityname%", city.getCityName(), "%enter%", city.getEnterMessage());
        }

        Utilities.sendCityScoreboard(event.getPlayer(), city, null);

        event.getPlayer().sendMessage("You have entered " + event.getCity().getCityName());
    }

    @EventHandler
    public void onPlayerExitCity(PlayerExitCityEvent event) {
        City city = event.getCity();
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
        Utilities.sendCityScoreboard(event.getPlayer(), event.getPlot().getCity(), event.getPlot());
    }

    @EventHandler
    public void onPlayerExitPlot(PlayerExitPlotEvent event) {
        event.getPlayer().sendMessage("You have exited the plot " + event.getPlot().getPlotName());
        Utilities.sendCityScoreboard(event.getPlayer(), event.getPlot().getCity(), null);
    }


}
