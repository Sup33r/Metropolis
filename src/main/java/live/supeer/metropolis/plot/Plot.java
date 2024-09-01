package live.supeer.metropolis.plot;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import live.supeer.metropolis.Database;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.utils.LocationUtil;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import org.bukkit.World;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class Plot {
    private static final GeometryFactory geometryFactory = new GeometryFactory();


    private final int plotId;
    private final int cityId;
    private String plotName;
    private String plotOwnerUUID;
    private int plotYMin;
    private int plotYMax;
    private String plotType;
    private boolean kMarked;
    private boolean isForSale;
    private int plotPrice;
    private int plotRent;
    private char[] permsMembers;
    private char[] permsOutsiders;
    private Location plotCenter;
    private char[] plotFlags;
    private final long plotCreationDate;
    private final World plotWorld;
    private Polygon plotPoints;
    private boolean isJail;
    private boolean hasLeaderboard;
    private boolean leaderboardShown;
    private final City city;

    public Plot(DbRow data) {
        this.plotId = data.getInt("plotId");
        this.cityId = data.getInt("cityId");
        this.plotName = data.getString("plotName");
        this.plotOwnerUUID = data.getString("plotOwnerUUID");
        this.plotYMin = data.getInt("plotYMin");
        this.plotYMax = data.getInt("plotYMax");
        this.plotType = data.getString("plotType");
        this.kMarked = data.get("plotKMarked");
        this.isForSale = data.get("plotIsForSale");
        this.plotPrice = data.getInt("plotPrice");
        this.plotRent = data.getInt("plotRent");
        if (CityDatabase.getCity(cityId).isPresent()) {
            this.city = CityDatabase.getCity(cityId).get();
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
        this.plotCenter = LocationUtil.stringToLocation(data.getString("plotCenter"));
        this.plotFlags =
                data.getString("plotFlags") == null
                        ? new char[0]
                        : data.getString("plotFlags").toCharArray();
        this.plotCreationDate = data.getLong("plotCreationDate");
        this.plotPoints = LocationUtil.stringToPolygon(data.getString("plotPoints"));
        this.plotWorld = plotCenter.getWorld();
        this.isJail = plotType != null && plotType.equalsIgnoreCase("jail");
        this.hasLeaderboard = data.get("leaderboard");
        this.leaderboardShown = data.get("leaderboardShown");
    }

    public void setLeaderboard(boolean hasLeaderboard) {
        this.hasLeaderboard = hasLeaderboard;
        try {
        DB.executeUpdate("UPDATE `mp_plots` SET `leaderboard` = " + hasLeaderboard + " WHERE `plotId` = " + plotId + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setLeaderboardShown(boolean leaderboardShown) {
        this.leaderboardShown = leaderboardShown;
        try {
            DB.executeUpdate("UPDATE `mp_plots` SET `leaderboardShown` = " + leaderboardShown + " WHERE `plotId` = " + plotId + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setPlotName(String plotName) {
        this.plotName = plotName;
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotName` = "
                        + Database.sqlString(plotName)
                        + " WHERE `plotId` = "
                        + plotId
                        + ";");
    }

    public void setPlotType(String plotType) {
        this.plotType = plotType;
        this.isJail = plotType != null && plotType.equalsIgnoreCase("jail");
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotType` = "
                        + Database.sqlString(plotType)
                        + " WHERE `plotId` = "
                        + plotId
                        + ";");
    }

    public void setPlotOwnerUUID(String plotOwnerUUID) {
        this.plotOwnerUUID = plotOwnerUUID;
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotOwnerUUID` = "
                        + Database.sqlString(plotOwnerUUID)
                        + " WHERE `plotId` = "
                        + plotId
                        + ";");
    }

    public void setForSale(boolean isForSale) {
        this.isForSale = isForSale;
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotIsForSale` = "
                        + isForSale
                        + " WHERE `plotId` = "
                        + plotId
                        + ";");
    }

    public void setPlotPrice(int plotPrice) {
        this.plotPrice = plotPrice;
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotPrice` = " + plotPrice + " WHERE `plotId` = " + plotId + ";");
    }

    public void setPlotRent(int plotRent) {
        this.plotRent = plotRent;
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotRent` = " + plotRent + " WHERE `plotId` = " + plotId + ";");
    }

    public void removePlotOwner() {
        this.plotOwnerUUID = null;
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotOwnerUUID` = NULL WHERE `plotId` = "
                        + plotId
                        + ";");
    }

    public void updatePlot(Player player, Location[] plotPoints, int minY, int maxY) {
        Polygon plotPolygon = LocationUtil.createPolygonFromLocations(plotPoints, geometryFactory);
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
        int centerX = (int) (plotPolygon.getEnvelopeInternal().getMinX() + plotPolygon.getEnvelopeInternal().getWidth() / 2);
        int centerZ = (int) (plotPolygon.getEnvelopeInternal().getMinY() + plotPolygon.getEnvelopeInternal().getHeight() / 2);
        Location plotCenter =
                new Location(
                        plotPoints[0].getWorld(),
                        centerX,
                        player.getWorld().getHighestBlockYAt(centerX, centerZ) + 1,
                        centerZ);
        try {
            DB.executeUpdate(
                    "UPDATE `mp_plots` SET `plotPoints` = "
                            + Database.sqlString(LocationUtil.polygonToString(plotPolygon))
                            + ", `plotYMin` = "
                            + minY
                            + ", `plotYMax` = "
                            + maxY
                            + ", `plotCenter` = "
                            + Database.sqlString(LocationUtil.locationToString(plotCenter))
                            + ", `plotBoundary` = ST_GeomFromText('"
                            + plotPolygon.toText()
                            + "')"
                            + " WHERE `plotId` = "
                            + plotId
                            + ";");

            // Update local instance variables
            this.plotPoints = plotPolygon;
            this.plotYMin = minY;
            this.plotYMax = maxY;
            this.plotCenter = plotCenter;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPlotPerms(String type, String perms, String playerUUID) {
        try {
            if (type.equalsIgnoreCase("members")) {
                DB.executeUpdate(
                        "UPDATE `mp_plots` SET `plotPermsMembers` = "
                                + Database.sqlString(perms)
                                + " WHERE `plotId` = "
                                + plotId
                                + ";");
                this.permsMembers = perms.toCharArray();
                return;
            } else if (type.equalsIgnoreCase("outsiders")) {
                DB.executeUpdate(
                        "UPDATE `mp_plots` SET `plotPermsOutsiders` = "
                                + Database.sqlString(perms)
                                + " WHERE `plotId` = "
                                + plotId
                                + ";");
                this.permsOutsiders = perms.toCharArray();
                return;
            }
            DB.executeUpdate(
                    "INSERT INTO `mp_plotperms` (`plotId`, `cityId`, `plotPerms`, `playerUUID`) VALUES ("
                            + plotId
                            + ", "
                            + cityId
                            + ", "
                            + Database.sqlString(perms)
                            + ", "
                            + Database.sqlString(playerUUID)
                            + ") ON DUPLICATE KEY UPDATE plotPerms = '"
                            + perms
                            + "';");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removePlotPerms() {
        try {
            DB.executeUpdate(
                    "UPDATE `mp_plots` SET `plotPermsMembers` = '', `plotPermsOutsiders` = '' WHERE `plotId` = "
                            + plotId
                            + ";");
            DB.executeUpdate("DELETE FROM `mp_plotperms` WHERE `plotId` = " + plotId + ";");
            this.permsMembers = new char[] {' '};
            this.permsOutsiders = new char[] {' '};
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                        + " WHERE `plotId` = "
                        + plotId
                        + ";");
    }

    public void setKMarked(boolean kMarked) {
        this.kMarked = kMarked;
        DB.executeUpdateAsync(
                "UPDATE `mp_plots` SET `plotKMarked` = " + kMarked + " WHERE `plotId` = " + plotId + ";");
    }

    public List<PlotPerms> getPlayerPlotPerms() {
        List<PlotPerms> plotPermsList = new ArrayList<>();
        try {
            List<DbRow> rows =
                    DB.getResults("SELECT * FROM `mp_plotperms` WHERE `plotId` = " + plotId + ";");
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
        DbRow row = null;
        try {
            row = DB.getFirstRow(
                    "SELECT * FROM `mp_plotperms` WHERE `plotId` = " + plotId + " AND `playerUUID` = " + Database.sqlString(playerUUID) + ";"
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (row == null) {
            return null; // Return null if no data is found
        }
        return new PlotPerms(row);
    }

    public List<Player> playersInPlot() {
        List<Player> players = new ArrayList<>();
        for (UUID uuid: Metropolis.playerInPlot.keySet()) {
            if (Metropolis.playerInPlot.get(uuid).getPlotId() == plotId) {
                players.add(Bukkit.getPlayer(uuid));
            }
        }
        return players;
    }

    public boolean contains(Location location) {
        if (!location.getWorld().equals(plotWorld)) {
            return false;
        }
        Point point = geometryFactory.createPoint(new Coordinate(location.getBlockX(), location.getBlockZ()));
        return plotPoints.covers(point) && location.getBlockY() >= plotYMin && location.getBlockY() <= plotYMax;
    }
}
