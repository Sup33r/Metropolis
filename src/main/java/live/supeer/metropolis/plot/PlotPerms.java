package live.supeer.metropolis.plot;

import co.aikar.idb.DbRow;
import lombok.Getter;

@Getter
public class PlotPerms {
    private final int plotID;
    private final int cityID;
    private final char[] perms;
    private final String playerName;
    private final String playerUUID;

    public PlotPerms(DbRow data) {
        this.plotID = data.getInt("plotId");
        this.cityID = data.getInt("cityId");
        this.perms =
                data.getString("plotPerms") == null
                        ? new char[0]
                        : data.getString("plotPerms").toCharArray();
        this.playerName = data.getString("playerName");
        this.playerUUID = data.getString("playerUUID");
    }
}
