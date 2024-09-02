package live.supeer.metropolis.city;

import co.aikar.idb.DbRow;
import lombok.Getter;

@Getter
public class CityPerms {
    private final int cityId;
    private final City city;
    private final char[] perms;
    private final String playerUUID;


    public CityPerms(DbRow data) {
        this.cityId = data.getInt("cityId");
        this.perms = data.getString("cityPerms") == null ? new char[0] : data.getString("cityPerms").toCharArray();
        this.playerUUID = data.getString("playerUUID");
        if (CityDatabase.getCity(cityId).isPresent()) {
            this.city = CityDatabase.getCity(cityId).get();
        } else {
            this.city = null;
        }
    }

    public CityPerms(int cityId, String perms, String playerUUID) {
        this.cityId = cityId;
        this.perms = perms == null ? new char[0] : perms.toCharArray();
        this.playerUUID = playerUUID;
        if (CityDatabase.getCity(cityId).isPresent()) {
            this.city = CityDatabase.getCity(cityId).get();
        } else {
            this.city = null;
        }
    }
}
