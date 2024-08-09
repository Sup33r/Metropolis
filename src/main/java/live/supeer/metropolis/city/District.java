package live.supeer.metropolis.city;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import live.supeer.metropolis.Database;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.utils.LocationUtil;
import live.supeer.metropolis.utils.Utilities;
import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.List;

@Getter
public class District {
    public static Metropolis plugin;
    private static final GeometryFactory geometryFactory = new GeometryFactory();


    private final String districtName;
    private final City city;
    private final List<OfflinePlayer> contactplayers;
    private final Polygon districtPoints;
    private final World world;

    public District(DbRow data) {
        this.districtName = data.getString("districtName");
        this.city = CityDatabase.getCity(data.getInt("cityId")).get();
        this.districtPoints = LocationUtil.stringToPolygon(data.getString("districtPoints"));
        String contactPlayersString = data.getString("contactPlayers");
        this.contactplayers = contactPlayersString != null ? Utilities.stringToOfflinePlayerList(contactPlayersString) : List.of();
        this.world = plugin.getServer().getWorld(data.getString("world"));
    }

    public boolean isPointInDistrict(double x, double z) {
        return districtPoints.contains(geometryFactory.createPoint(new Coordinate(x, z)));
    }

    public List<OfflinePlayer> addContactPlayer(OfflinePlayer player) {
        contactplayers.add(player);
        DB.executeUpdateAsync(
                "UPDATE `mp_districts` SET `contactPlayers` = "
                        + Database.sqlString(Utilities.offlinePlayerListToString(contactplayers))
                        + " WHERE `districtName` = "
                        + Database.sqlString(districtName) + " AND `cityID` = " + city.getCityID());
        return contactplayers;
    }

    public List<OfflinePlayer> removeContactPlayer(OfflinePlayer player) {
        contactplayers.remove(player);
        DB.executeUpdateAsync(
                "UPDATE `mp_districts` SET `contactPlayers` = "
                        + Database.sqlString(Utilities.offlinePlayerListToString(contactplayers))
                        + " WHERE `districtName` = "
                        + Database.sqlString(districtName) + " AND `cityID` = " + city.getCityID());
        return contactplayers;
    }

    public void setDistrictName(String districtName) {
        DB.executeUpdateAsync(
                "UPDATE `mp_districts` SET `districtName` = "
                        + Database.sqlString(districtName)
                        + " WHERE `districtName` = "
                        + Database.sqlString(this.districtName) + " AND `cityID` = " + city.getCityID());
    }
}
