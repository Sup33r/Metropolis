package live.supeer.metropolis.homecity;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import live.supeer.metropolis.Database;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HCDatabase {
    public static void setHomeCity(String uuid, City city) {
        try {
            if (hasHomeCity(uuid)) {
                DB.executeInsert(
                        "INSERT INTO mp_homecities (playerUUID, cityId) VALUES ("
                                + Database.sqlString(uuid)
                                + ", "
                                + city.getCityId()
                                + ");");
                return;
            }
            DB.executeUpdate("UPDATE mp_homecities SET cityId = " + city.getCityId() + " WHERE playerUUID = " + Database.sqlString(uuid));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int getHomeCityToCityId(String uuid) {
        try {
            var row = DB.getFirstRow("SELECT `cityId` FROM `mp_homecities` WHERE `playerUUID` = " + Database.sqlString(uuid) + ";");
            if (row == null || row.isEmpty()) {
                return -1;
            }
            return row.getInt("cityId");
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static List<String> getPlayerHomeCities(UUID uuid) {
        List<String> cityNames = new ArrayList<>();
        try {
            List<DbRow> rows = DB.getResults("SELECT `cityId` FROM `mp_homecities` WHERE `playerUUID` = " + Database.sqlString(uuid.toString()));
            for (DbRow row : rows) {
                int cityId = row.getInt("cityId");
                CityDatabase.getCity(cityId).ifPresent(city -> cityNames.add(city.getCityName()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cityNames;
    }

    public static City getHomeCityToCity(String uuid) {
        try {
            int cityId = getHomeCityToCityId(uuid);
            if (cityId == -1) return null;
            return CityDatabase.getCity(cityId).orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean hasHomeCity(String uuid) {
        try {
            return DB.getResults("SELECT * FROM `mp_homecities` WHERE `playerUUID` = " + Database.sqlString(uuid)).isEmpty();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static void removeHomeCity(String uuid, City city) {
        try {
            DB.executeUpdate("DELETE FROM `mp_homecities` WHERE `playerUUID` = " + Database.sqlString(uuid) + " AND `cityId` = " + city.getCityId() + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
