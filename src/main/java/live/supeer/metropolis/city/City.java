package live.supeer.metropolis.city;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import live.supeer.metropolis.Database;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.Utilities;
import live.supeer.metropolis.plot.Plot;
import lombok.Getter;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

@Getter
public class City {

    public static Metropolis plugin;

    private final int cityID;
    private String cityName;
    private final String originalMayorName;
    private final String originalMayorUUID;
    private final List<Member> cityMembers = new ArrayList<>();
    private final List<Claim> cityClaims = new ArrayList<>();
    private final List<Plot> cityPlots = new ArrayList<>();
    private int cityBalance;
    private Location citySpawn;
    private final long cityCreationDate;
    private String enterMessage;
    private String exitMessage;
    private String motdMessage;
    private final boolean isOpen;
    private boolean isRemoved;

    public City(DbRow data) {
        this.cityID = data.getInt("cityID");
        this.cityName = data.getString("cityName");
        this.originalMayorName = data.getString("originalMayorName");
        this.originalMayorUUID = data.getString("originalMayorUUID");
        this.cityBalance = data.getInt("cityBalance");
        this.citySpawn = Utilities.stringToLocation(data.getString("citySpawn"));
        this.cityCreationDate = data.getInt("createDate");
        this.enterMessage = data.getString("enterMessage");
        this.exitMessage = data.getString("exitMessage");
        this.motdMessage = data.getString("motdMessage");
        this.isOpen = data.get("isOpen");
        this.isRemoved = data.get("isRemoved");
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `cityName` = "
                        + Database.sqlString(cityName)
                        + " WHERE `cityID` = "
                        + cityID
                        + ";");
    }

    public void addCityBalance(int cityBalance) {
        this.cityBalance += cityBalance;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `cityBalance` = "
                        + this.cityBalance
                        + " WHERE `cityID` = "
                        + cityID
                        + ";");
    }

    public void removeCityBalance(int cityBalance) {
        this.cityBalance -= cityBalance;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `cityBalance` = "
                        + this.cityBalance
                        + " WHERE `cityID` = "
                        + cityID
                        + ";");
    }

    public void removeCityMember(Member member) {
        this.cityMembers.remove(member);
        DB.executeUpdateAsync(
                "DELETE FROM `mp_members` WHERE `cityID` = "
                        + cityID
                        + " AND `playerUUID` = "
                        + Database.sqlString(member.getPlayerUUID())
                        + ";");
    }

    public void setCitySpawn(Location citySpawn) {
        this.citySpawn = citySpawn;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `citySpawn` = "
                        + Database.sqlString(Utilities.locationToString(citySpawn))
                        + " WHERE `cityID` = "
                        + cityID
                        + ";");
    }

    public void setCityStatus(boolean isRemoved) {
        this.isRemoved = isRemoved;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `isRemoved` = "
                        + (isRemoved ? 1 : 0)
                        + " WHERE `cityID` = "
                        + cityID
                        + ";");
    }

    public void setEnterMessage(String enterMessage) {
        this.enterMessage = enterMessage;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `enterMessage` = "
                        + Database.sqlString(enterMessage)
                        + " WHERE `cityID` = "
                        + cityID
                        + ";");
    }

    public void setExitMessage(String exitMessage) {
        this.exitMessage = exitMessage;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `exitMessage` = "
                        + Database.sqlString(exitMessage)
                        + " WHERE `cityID` = "
                        + cityID
                        + ";");
    }

    public void setMotdMessage(String motdMessage) {
        this.motdMessage = motdMessage;
        DB.executeUpdateAsync(
                "UPDATE `mp_cities` SET `motdMessage` = "
                        + Database.sqlString(motdMessage)
                        + " WHERE `cityID` = "
                        + cityID
                        + ";");
    }

    public void addCityMember(Member member) {
        cityMembers.add(member);
    }

    public void addCityClaim(Claim claim) {
        cityClaims.add(claim);
    }

    public Claim getCityClaim(Location location) {
        for (Claim claim : cityClaims) {
            if (claim.getClaimWorld().equals(location.getWorld().toString())
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
