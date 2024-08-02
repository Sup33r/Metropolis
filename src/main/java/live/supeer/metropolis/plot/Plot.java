package live.supeer.metropolis.plot;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import live.supeer.metropolis.Database;
import live.supeer.metropolis.Utilities;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class Plot {

    private final int plotID;
    private final int cityID;
    private String plotName;
    private String plotOwner;
    private String plotOwnerUUID;
    private final int plotYMin;
    private final int plotYMax;
    private String plotType;
    private boolean kMarked;
    private boolean isForSale;
    private int plotPrice;
    private int plotRent;
    private char[] permsMembers;
    private char[] permsOutsiders;
    private final Location plotCenter;
    private char[] plotFlags;
    private final long plotCreationDate;
    private final Location[] plotPoints;
    private final City city;

    public Plot(DbRow data) {
        this.plotID = data.getInt("plotId");
        this.cityID = data.getInt("cityId");
        this.plotName = data.getString("plotName");
        this.plotOwner = data.getString("plotOwner");
        this.plotOwnerUUID = data.getString("plotOwnerUUID");
        this.plotYMin = data.getInt("plotYMin");
        this.plotYMax = data.getInt("plotYMax");
        this.plotType = data.getString("plotType");
        this.kMarked = data.get("plotKMarked");
        this.isForSale = data.get("plotIsForSale");
        this.plotPrice = data.getInt("plotPrice");
        this.plotRent = data.getInt("plotRent");
        if (CityDatabase.getCity(cityID).isPresent()) {
            this.city = CityDatabase.getCity(cityID).get();
        } else {
            this.city = null;
        }
        this.permsMembers =
                data.getString("plotPermsMembers") == null
                        ? new char[0]
                        : data.getString("plotPermsMembers").toCharArray();
        this.permsOutsiders =
                data.getString("plotPermsOutsiders") == null
                        ? new char[0]
                        : data.getString("plotPermsOutsiders").toCharArray();
        this.plotCenter = Utilities.stringToLocation(data.getString("plotCenter"));
        this.plotFlags =
                data.getString("plotFlags") == null
                        ? new char[0]
                        : data.getString("plotFlags").toCharArray();
        this.plotCreationDate = data.getLong("plotCreationDate");
        this.plotPoints = Utilities.stringToPolygon(data.getString("plotPoints"));
    }

    public void setPlotName(String plotName) {
        this.plotName = plotName;
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotName` = "
                        + Database.sqlString(plotName)
                        + " WHERE `plotID` = "
                        + plotID
                        + ";");
    }

    public void setPlotType(String plotType) {
        this.plotType = plotType;
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotType` = "
                        + Database.sqlString(plotType)
                        + " WHERE `plotID` = "
                        + plotID
                        + ";");
    }

    public void setPlotOwner(String plotOwner) {
        this.plotOwner = plotOwner;
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotOwner` = "
                        + Database.sqlString(plotOwner)
                        + " WHERE `plotID` = "
                        + plotID
                        + ";");
    }

    public void setPlotOwnerUUID(String plotOwnerUUID) {
        this.plotOwnerUUID = plotOwnerUUID;
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotOwnerUUID` = "
                        + Database.sqlString(plotOwnerUUID)
                        + " WHERE `plotID` = "
                        + plotID
                        + ";");
    }

    public void setForSale(boolean isForSale) {
        this.isForSale = isForSale;
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotIsForSale` = "
                        + isForSale
                        + " WHERE `plotID` = "
                        + plotID
                        + ";");
    }

    public void setPlotPrice(int plotPrice) {
        this.plotPrice = plotPrice;
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotPrice` = " + plotPrice + " WHERE `plotID` = " + plotID + ";");
    }

    public void setPlotRent(int plotRent) {
        this.plotRent = plotRent;
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotRent` = " + plotRent + " WHERE `plotID` = " + plotID + ";");
    }

    public void removePlotOwner() {
        this.plotOwner = null;
        this.plotOwnerUUID = null;
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotOwner` = NULL, `plotOwnerUUID` = NULL WHERE `plotID` = "
                        + plotID
                        + ";");
    }

    public void updatePlot(Player player, Location[] plotPoints, int minY, int maxY) {
        Polygon plotPolygon = new Polygon();
        for (Location plotPoint : plotPoints) {
            plotPolygon.addPoint(plotPoint.getBlockX(), plotPoint.getBlockZ());
        }
        if (minY == 0 && maxY == 0) {
            for (Location plotPoint : plotPoints) {
                if (plotPoint.getBlockY() < minY) {
                    minY = plotPoint.getBlockY();
                }
                if (plotPoint.getBlockY() > maxY) {
                    maxY = plotPoint.getBlockY();
                }
            }
        }
        int centerX = plotPolygon.getBounds().x + plotPolygon.getBounds().width / 2;
        int centerZ = plotPolygon.getBounds().y + plotPolygon.getBounds().height / 2;
        Location plotCenter =
                new Location(
                        plotPoints[0].getWorld(),
                        centerX,
                        player.getWorld().getHighestBlockYAt(centerX, centerZ) + 1,
                        centerZ);
        try {
            DB.executeUpdate(
                    "UPDATE `mp_plots` SET `plotPoints` = "
                            + Database.sqlString(Utilities.polygonToString(plotPoints))
                            + ", `plotYMin` = "
                            + minY
                            + ", `plotYMax` = "
                            + maxY
                            + ", `plotCenter` = "
                            + Database.sqlString(Utilities.locationToString(plotCenter))
                            + " WHERE `plotId` = "
                            + plotID
                            + ";");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPlotPerms(String type, String perms, String playerUUID) throws SQLException {
        if (type.equalsIgnoreCase("members")) {
            DB.executeUpdate(
                    "UPDATE `mp_plots` SET `plotPermsMembers` = "
                            + Database.sqlString(perms)
                            + " WHERE `plotID` = "
                            + plotID
                            + ";");
            this.permsMembers = perms.toCharArray();
            return;
        } else if (type.equalsIgnoreCase("outsiders")) {
            DB.executeUpdate(
                    "UPDATE `mp_plots` SET `plotPermsOutsiders` = "
                            + Database.sqlString(perms)
                            + " WHERE `plotID` = "
                            + plotID
                            + ";");
            this.permsOutsiders = perms.toCharArray();
            return;
        }
        DB.executeUpdate(
                "INSERT INTO `mp_plotperms` (`plotId`, `cityId`, `plotPerms`, `playerUUID`, `playerName`) VALUES ("
                        + plotID
                        + ", "
                        + cityID
                        + ", "
                        + Database.sqlString(perms)
                        + ", "
                        + Database.sqlString(playerUUID)
                        + ", "
                        + Database.sqlString(Bukkit.getOfflinePlayer(UUID.fromString(playerUUID)).getName())
                        + ") ON DUPLICATE KEY UPDATE plotPerms = '"
                        + perms
                        + "';");
    }

    public void removePlotPerms() throws SQLException {
        DB.executeUpdate(
                "UPDATE `mp_plots` SET `plotPermsMembers` = '', `plotPermsOutsiders` = '' WHERE `plotID` = "
                        + plotID
                        + ";");
        DB.executeUpdate("DELETE FROM `mp_plotperms` WHERE `plotId` = " + plotID + ";");
        this.permsMembers = new char[] {' '};
        this.permsOutsiders = new char[] {' '};
    }

    public boolean hasFlag(char needle) {
        if (this.plotFlags == null) {
            return false;
        }

        for (char option : this.plotFlags) {
            if (option == needle) {
                return true;
            }
        }
        return false;
    }

    public void setPlotFlags(String flags) {
        this.plotFlags = flags.toCharArray();
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotFlags` = "
                        + Database.sqlString(flags)
                        + " WHERE `plotID` = "
                        + plotID
                        + ";");
    }

    public void setKMarked(boolean kMarked) {
        this.kMarked = kMarked;
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotKMarked` = " + kMarked + " WHERE `plotID` = " + plotID + ";");
    }

    public List<PlotPerms> getPlayerPlotPerms() {
        List<PlotPerms> plotPermsList = new ArrayList<>();
        try {
            List<DbRow> rows =
                    DB.getResults("SELECT * FROM `mp_plotperms` WHERE `plotId` = " + plotID + ";");
            for (DbRow row : rows) {
                plotPermsList.add(new PlotPerms(row));
            }
            return plotPermsList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public PlotPerms getPlayerPlotPerm(String playerUUID) {
        try {
            if (DB.getFirstRow(
                    "SELECT * FROM `mp_plotperms` WHERE `plotId` = "
                            + plotID
                            + " AND `playerUUID` = "
                            + Database.sqlString(playerUUID)
                            + ";")
                    == null) {
                return null;
            }
            DbRow row =
                    DB.getFirstRow(
                            "SELECT * FROM `mp_plotperms` WHERE `plotId` = "
                                    + plotID
                                    + " AND `playerUUID` = "
                                    + Database.sqlString(playerUUID)
                                    + ";");
            return new PlotPerms(row);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
