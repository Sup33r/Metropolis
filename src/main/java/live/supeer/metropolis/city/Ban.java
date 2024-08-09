package live.supeer.metropolis.city;

import co.aikar.idb.DbRow;
import lombok.Getter;

@Getter
public class Ban {
    private final int cityId;
    private final City city;
    private final String playerUUID;
    private final long placeDate;
    private final long length;
    private final String reason;
    private final String placeUUID;

    public Ban(DbRow data) {
        this.cityId = data.getInt("cityId");
        this.playerUUID = data.getString("playerUUID");
        this.placeDate = data.getLong("placeDate");
        this.length = data.getLong("length");
        this.reason = data.getString("reason");
        this.placeUUID = data.getString("placeUUID");
        if (CityDatabase.getCity(cityId).isPresent()) {
            this.city = CityDatabase.getCity(cityId).get();
        } else {
            this.city = null;
        }
    }
}
