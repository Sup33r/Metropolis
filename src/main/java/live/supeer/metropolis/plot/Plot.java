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
    private List<PlotPerms> plotPerms;
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
        this.plotFlags = data.getString("plotFlags") == null ? new char[0] : data.getString("plotFlags").toCharArray();
        this.plotCreationDate = data.getLong("plotCreationDate");
        this.plotPoints = LocationUtil.stringToPolygon(data.getString("plotPoints"));
        this.plotWorld = plotCenter.getWorld();
        this.isJail = plotType != null && plotType.equalsIgnoreCase("jail");
        this.hasLeaderboard = data.get("leaderboard");
        this.leaderboardShown = data.get("leaderboardShown");
        this.plotPerms = loadPlayerPlotPerms();
    }

    public void setLeaderboard(boolean hasLeaderboard) {
        this.hasLeaderboard = hasLeaderboard;
        try {
            DB.executeUpdate("UPDATE `mp_plots` SET `leaderboard` = ? WHERE `plotId` = ?", hasLeaderboard, plotId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setLeaderboardShown(boolean leaderboardShown) {
        this.leaderboardShown = leaderboardShown;
        try {
            DB.executeUpdate("UPDATE `mp_plots` SET `leaderboardShown` = ? WHERE `plotId` = ?", leaderboardShown, plotId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setPlotName(String plotName) {
        this.plotName = plotName;
        DB.executeUpdateAsync("UPDATE `mp_plots` SET `plotName` = ? WHERE `plotId` = ?", plotName, plotId);
    }

    public void setPlotType(String plotType) {
        this.plotType = plotType;
        this.isJail = plotType != null && plotType.equalsIgnoreCase("jail");
        DB.executeUpdateAsync("UPDATE `mp_plots` SET `plotType` = ? WHERE `plotId` = ?", plotType, plotId);
    }

    public void setPlotOwnerUUID(String plotOwnerUUID) {
        this.plotOwnerUUID = plotOwnerUUID;
        DB.executeUpdateAsync("UPDATE `mp_plots` SET `plotOwnerUUID` = ? WHERE `plotId` = ?", plotOwnerUUID, plotId);
    }

    public void setForSale(boolean isForSale) {
        this.isForSale = isForSale;
        DB.executeUpdateAsync("UPDATE `mp_plots` SET `plotIsForSale` = ? WHERE `plotId` = ?", isForSale, plotId);
    }

    public void setPlotPrice(int plotPrice) {
        this.plotPrice = plotPrice;
        DB.executeUpdateAsync("UPDATE `mp_plots` SET `plotPrice` = ? WHERE `plotId` = ?", plotPrice, plotId);
    }

    public void setPlotRent(int plotRent) {
        this.plotRent = plotRent;
        DB.executeUpdateAsync("UPDATE `mp_plots` SET `plotRent` = ? WHERE `plotId` = ?", plotRent, plotId);
    }

    public void removePlotOwner() {
        this.plotOwnerUUID = null;
        DB.executeUpdateAsync("UPDATE `mp_plots` SET `plotOwnerUUID` = NULL WHERE `plotId` = ?", plotId);
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
            DB.executeUpdate("UPDATE `mp_plots` SET `plotPoints` = ?, `plotYMin` = ?, `plotYMax` = ?, `plotCenter` = ?, `plotBoundary` = ST_GeomFromText(?) WHERE `plotId` = ?",
                    LocationUtil.polygonToString(plotPolygon),
                    minY,
                    maxY,
                    LocationUtil.locationToString(plotCenter),
                    plotPolygon.toText(),
                    plotId);
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
                DB.executeUpdate("UPDATE `mp_plots` SET `plotPermsMembers` = ? WHERE `plotId` = ?", perms, plotId);
                this.permsMembers = perms.toCharArray();
                return;
            } else if (type.equalsIgnoreCase("outsiders")) {
                DB.executeUpdate("UPDATE `mp_plots` SET `plotPermsOutsiders` = ? WHERE `plotId` = ?", perms, plotId);
                this.permsOutsiders = perms.toCharArray();
                return;
            }
            DB.executeInsert("INSERT INTO `mp_plotperms` (plotId, cityId, plotPerms, playerUUID) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE plotPerms = ?", plotId, cityId, perms, playerUUID, perms);
            PlotPerms plotPerm = new PlotPerms(plotId, cityId, perms, playerUUID);
            plotPerms.removeIf(p -> p.getPlayerUUID().equals(playerUUID));
            plotPerms.add(plotPerm);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removePlotPerms() {
        try {
            DB.executeUpdate("UPDATE `mp_plots` SET `plotPermsMembers` = '', `plotPermsOutsiders` = '' WHERE `plotId` = ?", plotId);
            DB.executeUpdate("DELETE FROM `mp_plotperms` WHERE `plotId` = ?", plotId);
            this.permsMembers = new char[] {' '};
            this.permsOutsiders = new char[] {' '};
            this.plotPerms.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<PlotPerms> loadPlayerPlotPerms() {
        List<PlotPerms> plotPermsList = new ArrayList<>();
        try {
            List<DbRow> rows =
                    DB.getResults("SELECT * FROM `mp_plotperms` WHERE `plotId` = ?", plotId);
            for (DbRow row : rows) {
                plotPermsList.add(new PlotPerms(row));
            }
            this.plotPerms = plotPermsList;
            return plotPermsList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public PlotPerms getPlayerPlotPerm(String playerUUID) {
        for (PlotPerms perm : plotPerms) {
            if (perm.getPlayerUUID().equals(playerUUID)) {
                return perm;
            }
        }
        return null;
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
        DB.executeUpdateAsync("UPDATE `mp_plots` SET `plotFlags` = ? WHERE `plotId` = ?", flags, plotId);
    }

    public void setKMarked(boolean kMarked) {
        this.kMarked = kMarked;
        DB.executeUpdateAsync("UPDATE `mp_plots` SET `plotKMarked` = ? WHERE `plotId` = ?", kMarked, plotId);
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
