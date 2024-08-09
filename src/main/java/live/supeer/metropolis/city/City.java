package live.supeer.metropolis.city;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import live.supeer.metropolis.Database;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.utils.LocationUtil;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.utils.Utilities;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.STRtree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class City {

    public static Metropolis plugin;

    private final int cityId;
    private String cityName;
    private final String originalMayorName;
    private final String originalMayorUUID;
    private final List<Member> cityMembers = new ArrayList<>();
    private final List<Claim> cityClaims = new ArrayList<>();
    private final List<Plot> cityPlots = new ArrayList<>();
    private List<City> twinCities = new ArrayList<>();
//    private final List<Ban> cityBans = new ArrayList<>();
    private int minChunkDistance;
    private int minSpawnDistance;
    private int bonusClaims;
    private int cityBalance;
    private Location citySpawn;
    private final long cityCreationDate;
    private String enterMessage;
    private String exitMessage;
    private String motdMessage;
    private boolean isOpen;
    private boolean isPublic;
    private double cityTax;

    public City(DbRow data) {
        this.cityId = data.getInt("cityId");
        this.cityName = data.getString("cityName");
        this.originalMayorName = data.getString("originalMayorName");
        this.originalMayorUUID = data.getString("originalMayorUUID");
        this.cityBalance = data.getInt("cityBalance");
        this.citySpawn = LocationUtil.stringToLocation(data.getString("citySpawn"));
        this.cityCreationDate = data.getLong("createDate");
        this.enterMessage = data.getString("enterMessage");
        this.exitMessage = data.getString("exitMessage");
        this.motdMessage = data.getString("motdMessage");
        this.isOpen = data.get("isOpen");
        this.isPublic = data.get("isPublic");
        this.bonusClaims = data.getInt("bonusClaims");
        this.minChunkDistance = data.getInt("minChunkDistance");
        this.minSpawnDistance = data.getInt("minSpawnDistance");
        this.cityTax = data.getDbl("cityTax");
        this.twinCities = new ArrayList<>(Utilities.stringToCityList(data.getString("twinCities")));
    }

    public void addTwinCity(City city) {
        twinCities.add(city);
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `twinCities` = "
                        + Database.sqlString(Utilities.cityListToString(twinCities))
                        + " WHERE `cityId` = "
                        + cityId
                        + ";");
    }

    public void removeTwinCity(City city) {
        twinCities.remove(city);
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `twinCities` = "
                        + Database.sqlString(Utilities.cityListToString(twinCities))
                        + " WHERE `cityId` = "
                        + cityId
                        + ";");
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `cityName` = "
                        + Database.sqlString(cityName)
                        + " WHERE `cityId` = "
                        + cityId
                        + ";");
    }

    public void addCityBalance(int cityBalance) {
        this.cityBalance += cityBalance;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `cityBalance` = "
                        + this.cityBalance
                        + " WHERE `cityId` = "
                        + cityId
                        + ";");
    }

    public void removeCityBalance(int cityBalance) {
        this.cityBalance -= cityBalance;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `cityBalance` = "
                        + this.cityBalance
                        + " WHERE `cityId` = "
                        + cityId
                        + ";");
    }

    public void setMinChunkDistance(int minChunkDistance) {
        this.minChunkDistance = minChunkDistance;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `minChunkDistance` = "
                        + minChunkDistance
                        + " WHERE `cityId` = "
                        + cityId
                        + ";");
    }

    public void setMinSpawnDistance(int minSpawnDistance) {
        this.minSpawnDistance = minSpawnDistance;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `minSpawnDistance` = "
                        + minSpawnDistance
                        + " WHERE `cityId` = "
                        + cityId
                        + ";");
    }

    public void removeCityMember(Member member) {
        this.cityMembers.remove(member);
        HCDatabase.removeHomeCity(member.getPlayerUUID(), this);
        DB.executeUpdateAsync("DELETE FROM `mp_members` WHERE `cityId` = " + cityId + " AND `playerUUID` = " + Database.sqlString(member.getPlayerUUID()) + ";");
    }

    public void setCitySpawn(Location citySpawn) {
        this.citySpawn = citySpawn;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `citySpawn` = "
                        + Database.sqlString(LocationUtil.locationToString(citySpawn))
                        + " WHERE `cityId` = "
                        + cityId
                        + ";");
    }

    public void setCityTax(double cityTax) {
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `cityTax` = "
                        + cityTax
                        + " WHERE `cityId` = "
                        + cityId
                        + ";");
        this.cityTax = cityTax;
    }

    public void addBonusClaims(int bonusClaims) {
        this.bonusClaims += bonusClaims;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `bonusClaims` = "
                        + this.bonusClaims
                        + " WHERE `cityId` = "
                        + cityId
                        + ";");
    }

    public void setEnterMessage(String enterMessage) {
        this.enterMessage = enterMessage;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `enterMessage` = "
                        + Database.sqlString(enterMessage)
                        + " WHERE `cityId` = "
                        + cityId
                        + ";");
    }

    public void setExitMessage(String exitMessage) {
        this.exitMessage = exitMessage;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `exitMessage` = "
                        + Database.sqlString(exitMessage)
                        + " WHERE `cityId` = "
                        + cityId
                        + ";");
    }

    public void setMotdMessage(String motdMessage) {
        this.motdMessage = motdMessage;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `motdMessage` = "
                        + Database.sqlString(motdMessage)
                        + " WHERE `cityId` = "
                        + cityId
                        + ";");
    }

    public void addCityMember(Member member) {
        cityMembers.add(member);
    }

    public Member getCityMember(String playerUUID) {
        for (Member member : cityMembers) {
            if (member.getPlayerUUID().equals(playerUUID)) {
                return member;
            }
        }
        return null;
    }

    public boolean toggleOpen() {
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `isOpen` = "
                        + (isOpen ? 0 : 1)
                        + " WHERE `cityId` = "
                        + cityId
                        + ";");
        return isOpen = !isOpen;
    }

    public boolean togglePublic() {
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `isPublic` = "
                        + (isPublic ? 0 : 1)
                        + " WHERE `cityId` = "
                        + cityId
                        + ";");
        return isPublic = !isPublic;
    }

//    public void addCityBan(Ban ban) {
//        cityBans.add(ban);
//    }
//
//    public void removeCityBan(Ban ban) {
//        cityBans.remove(ban);
//    }

    public void addCityClaim(Claim claim) {
        cityClaims.add(claim);
    }

    public void removeCityClaim(Claim claim) {
        cityClaims.remove(claim);
    }

    public Claim getCityClaim(Location location) {
        for (Claim claim : cityClaims) {
            if (claim.getClaimWorld().equals(location.getWorld())
                    && claim.getXPosition() == location.getChunk().getX()
                    && claim.getZPosition() == location.getChunk().getZ()) {
                return claim;
            }
        }
        return null;
    }

    public void addCityPlot(Plot plot) {
        cityPlots.add(plot);
    }
}
