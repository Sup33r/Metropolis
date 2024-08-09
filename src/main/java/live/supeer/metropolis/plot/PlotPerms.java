package live.supeer.metropolis.plot;

import co.aikar.idb.DbRow;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import lombok.Getter;

@Getter
public class PlotPerms {
    private final int plotId;
    private final int cityId;
    private final char[] perms;
    private final String playerName;
    private final String playerUUID;
    private final City city;


    public PlotPerms(DbRow data) {
        this.plotId = data.getInt("plotId");
        this.cityId = data.getInt("cityId");
        this.perms =
                data.getString("plotPerms") == null
                        ? new char[0]
                        : data.getString("plotPerms").toCharArray();
        this.playerName = data.getString("playerName");
        this.playerUUID = data.getString("playerUUID");
        if (CityDatabase.getCity(cityId).isPresent()) {
            this.city = CityDatabase.getCity(cityId).get();
        } else {
            this.city = null;
        }
    }
}
