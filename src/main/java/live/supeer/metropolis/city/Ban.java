package live.supeer.metropolis.city;

import co.aikar.idb.DbRow;
import lombok.Getter;

@Getter
public class Ban {
    private final int cityID;
    private final City city;
    private final String playerUUID;
    private final long placeDate;
    private final long expiryDate;
    private final String reason;
    private final String placeUUID;

    public Ban(DbRow data) {
        this.cityID = data.getInt("cityID");
        this.playerUUID = data.getString("playerUUID");
        this.placeDate = data.getInt("placeDate");
        this.expiryDate = data.getInt("expiryDate");
        this.reason = data.getString("reason");
        this.placeUUID = data.getString("placeUUID");
        if (CityDatabase.getCity(cityID).isPresent()) {
            this.city = CityDatabase.getCity(cityID).get();
        } else {
            this.city = null;
        }
    }
}
