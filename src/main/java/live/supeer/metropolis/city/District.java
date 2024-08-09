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
import org.bukkit.entity.Player;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

@Getter
public class District {
    public static Metropolis plugin;
    private static final GeometryFactory geometryFactory = new GeometryFactory();


    private final String districtName;
    private final City city;
    private List<OfflinePlayer> contactplayers;
    private Polygon districtPoints;
    private World world;

    public District(DbRow data) {
        this.districtName = data.getString("districtName");
        this.city = CityDatabase.getCity(data.getInt("cityId")).get();
        this.districtPoints = LocationUtil.stringToPolygon(data.getString("districtPoints"));
        String contactPlayersString = data.getString("contactPlayers");
        this.contactplayers = new ArrayList<>(Utilities.stringToOfflinePlayerList(contactPlayersString));
        this.world = plugin.getServer().getWorld(data.getString("world"));
    }

    public void addContactPlayer(OfflinePlayer player) {
        if (contactplayers == null) {
            contactplayers = new ArrayList<>();
        }
        contactplayers.add(player);
        DB.executeUpdateAsync(
                "UPDATE `mp_districts` SET `contactPlayers` = "
                        + Database.sqlString(Utilities.offlinePlayerListToString(contactplayers))
                        + " WHERE `districtName` = "
                        + Database.sqlString(districtName) + " AND `cityId` = " + city.getCityId());
    }

    public void removeContactPlayer(OfflinePlayer player) {
        contactplayers.remove(player);
        DB.executeUpdateAsync(
                "UPDATE `mp_districts` SET `contactPlayers` = "
                        + Database.sqlString(Utilities.offlinePlayerListToString(contactplayers))
                        + " WHERE `districtName` = "
                        + Database.sqlString(districtName) + " AND `cityId` = " + city.getCityId());
    }

    public void setDistrictName(String districtName) {
        DB.executeUpdateAsync(
                "UPDATE `mp_districts` SET `districtName` = "
                        + Database.sqlString(districtName)
                        + " WHERE `districtName` = "
                        + Database.sqlString(this.districtName) + " AND `cityId` = " + city.getCityId());
    }

    public void update(Player player, Polygon districtPolygon) {

        try {
            DB.executeUpdate(
                    "UPDATE `mp_districts` SET `districtPoints` = "
                            + Database.sqlString(LocationUtil.polygonToString(districtPolygon))
                            + ", `districtBoundary` = ST_GeomFromText('"
                            + districtPolygon.toText()
                            + "')"
                            + ", `world` = "
                            + Database.sqlString(player.getWorld().getName())
                            + " WHERE `districtName` = "
                            + Database.sqlString(districtName)
                            + " AND `cityId` = "
                            + city.getCityId()
                            + ";");

            this.districtPoints = districtPolygon;
            this.world = player.getWorld();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
