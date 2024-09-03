package live.supeer.metropolis.city;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import live.supeer.metropolis.Metropolis;
import lombok.Getter;

@Getter
public class Member {
    private final String playerUUID;
    private final int cityId;
    private final String cityName;
    private Role cityRole;
    private final long joinDate;
    private final City city;

    public Member(DbRow data) {
        this.playerUUID = data.getString("playerUUID");
        this.cityId = data.getInt("cityId");
        this.cityName = data.getString("cityName");
        this.cityRole = Role.fromString(data.getString("cityRole"));
        this.joinDate = data.getLong("joinDate");
        if (CityDatabase.getCity(cityId).isPresent()) {
            this.city = CityDatabase.getCity(cityId).get();
        } else {
            this.city = null;
        }
    }

    public void setRole(Role role) {
        this.cityRole = role;
        try {
            DB.executeUpdate("UPDATE cityMembers SET cityRole = ? WHERE playerUUID = ? AND cityId = ?", role.toString(), playerUUID, cityId);
        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}
