package live.supeer.metropolis.plot;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import live.supeer.metropolis.Database;
import live.supeer.metropolis.Leaderboard;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.Standing;
import live.supeer.metropolis.city.CityDatabase;
import live.supeer.metropolis.city.Claim;
import live.supeer.metropolis.utils.LocationUtil;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.utils.DateUtil;
import live.supeer.metropolis.utils.Utilities;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.List;
import java.util.UUID;

public class PlotDatabase {
    public static Plot createPlot(Player player, Polygon plotPolygon, String plotName, City city, int minY, int maxY, World world) {
        if (plotName == null) {
            int plotAmount = getPlotAmount() + 1;
            plotName = "Tomt #" + plotAmount;
        }



        if (minY == 0 && maxY == 0) {
            maxY = 256;
            Metropolis.getInstance().getLogger().warning("PlotDatabase.createPlot: minY and maxY was 0, setting to 0 and 256");
        }
        int centerX = (int) (plotPolygon.getEnvelopeInternal().getMinX() + plotPolygon.getEnvelopeInternal().getWidth() / 2);
        int centerZ = (int) (plotPolygon.getEnvelopeInternal().getMinY() + plotPolygon.getEnvelopeInternal().getHeight() / 2);
        Location plotCenter = new Location(world, centerX, player.getWorld().getHighestBlockYAt(centerX, centerZ) + 1, centerZ);
        try {
            DB.executeUpdate(
                    "INSERT INTO `mp_plots` (`cityId`, `cityName`, `plotName`, `plotOwnerUUID`, `plotPoints`, `plotYMin`, `plotYMax`, `plotPermsMembers`, `plotPermsOutsiders`, `plotCenter`, `plotCreationDate`, `plotBoundary`) VALUES ("
                            + city.getCityId()
                            + ", "
                            + Database.sqlString(city.getCityName())
                            + ", "
                            + Database.sqlString(plotName)
                            + ", "
                            + Database.sqlString(player.getUniqueId().toString())
                            + ", "
                            + Database.sqlString(LocationUtil.polygonToString(plotPolygon))
                            + ", "
                            + minY
                            + ", "
                            + maxY
                            + ", "
                            + "'gt'"
                            + ", "
                            + "'gt'"
                            + ", "
                            + Database.sqlString(LocationUtil.locationToString(plotCenter))
                            + ", "
                            + DateUtil.getTimestamp()
                            + ", "
                            + "ST_GeomFromText('"
                            + plotPolygon.toText()
                            + "')"
                            + ");");
            Plot plot = new Plot(DB.getFirstRow(
                                    "SELECT * FROM `mp_plots` WHERE `plotName` = "
                                            + Database.sqlString(plotName)
                                            + " AND `cityName` = "
                                            + Database.sqlString(city.getCityName())
                                            + ";"));
            city.addCityPlot(plot);
            return plot;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getPlotAmount() {
        try {
            return DB.getResults("SELECT * FROM `mp_plots`").size();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void deletePlot(Plot plot) {
        try {
            DB.executeUpdate("DELETE FROM `mp_plots` WHERE `plotId` = " + plot.getPlotId() + ";");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Plot getPlot(int id) {
        try {
            return new Plot(DB.getFirstRow("SELECT * FROM `mp_plots` WHERE `plotId` = " + id + ";"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean plotExists(int id) {
        try {
            return DB.getFirstRow("SELECT * FROM `mp_plots` WHERE `plotId` = " + id + ";") != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean intersectsExistingPlot(Polygon polygon, int yMin, int yMax, City city, World world) {
        String worldName = world.getName();
        try {
            String polygonWKT = polygon.toText();
            List<DbRow> results = DB.getResults(
                    "SELECT * FROM `mp_plots` WHERE ST_Intersects(`plotBoundary`, ST_GeomFromText(" + Database.sqlString(polygonWKT) + ")) AND `cityId` = " + city.getCityId() + " AND `plotYMin` <= " + yMax + " AND `plotYMax` >= " + yMin + " AND `plotCenter` LIKE " + Database.sqlString(worldName + "%") + ";");
            return !results.isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int getPlayerPlotCount(Player player) {
        try {
            return DB.getResults("SELECT * FROM `mp_plots` WHERE `plotOwnerUUID` = " + Database.sqlString(player.getUniqueId().toString()) + ";").size();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static Plot[] intersectingPlots(Polygon polygon, int yMin, int yMax, City city, World world) {
        String worldName = world.getName();
        try {
            String polygonWKT = polygon.toText();
            List<DbRow> results = DB.getResults(
                    "SELECT * FROM `mp_plots` WHERE ST_Intersects(`plotBoundary`, ST_GeomFromText(" + Database.sqlString(polygonWKT) + ")) AND `cityId` = " + city.getCityId() + " AND `plotYMin` <= " + yMax + " AND `plotYMax` >= " + yMin + " AND `plotCenter` LIKE " + Database.sqlString(worldName + "%") + ";");
            Plot[] plots = new Plot[results.size()];
            for (int i = 0; i < results.size(); i++) {
                plots[i] = new Plot(results.get(i));
            }
            return plots;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Plot getCityPlot(City city, Location location) {
        for (Plot plot : city.getCityPlots()) {
            if (plot.contains(location.toBlockLocation())) {
                return plot;
            }
        }
        return null;
    }

    public static boolean hasPlotInClaim(Claim claim) {
        World world = claim.getClaimWorld();
        int chunkX = claim.getXPosition();
        int chunkZ = claim.getZPosition();

        // Create a polygon representing the chunk
        GeometryFactory geometryFactory = new GeometryFactory();
        Coordinate[] coordinates = new Coordinate[5];
        coordinates[0] = new Coordinate(chunkX * 16, chunkZ * 16);
        coordinates[1] = new Coordinate((chunkX + 1) * 16, chunkZ * 16);
        coordinates[2] = new Coordinate((chunkX + 1) * 16, (chunkZ + 1) * 16);
        coordinates[3] = new Coordinate(chunkX * 16, (chunkZ + 1) * 16);
        coordinates[4] = coordinates[0]; // Close the polygon
        Polygon chunkPolygon = geometryFactory.createPolygon(coordinates);

        try {
            String polygonWKT = chunkPolygon.toText();
            List<DbRow> results = DB.getResults(
                    "SELECT COUNT(*) as count FROM `mp_plots` WHERE ST_Contains(ST_GeomFromText(?), ST_Centroid(`plotBoundary`)) AND `plotCenter` LIKE ?;",
                    polygonWKT,
                    world.getName() + "%"
            );

            if (!results.isEmpty()) {
                int count = results.get(0).getInt("count");
                return count > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static Leaderboard startLeaderboard(Plot plot, String type, Player player) {
        try {
            DB.executeUpdate("INSERT INTO `mp_leaderboards` (`plotId`, `creatorUUID`, `createDate`, `type`) VALUES (?, ?, ?, ?)", plot.getPlotId(), player.getUniqueId().toString(), DateUtil.getTimestamp(), type);
            DbRow row = DB.getFirstRow("SELECT * FROM `mp_leaderboards` WHERE `plotId` = ? AND `creatorUUID` = ?", plot.getPlotId(), player.getUniqueId().toString());
            return new Leaderboard(UUID.fromString(row.getString("creatorUUID")), row.getLong("createDate"), row.getString("type"), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateLeaderboard(Leaderboard leaderboard, int plotId) {
        try {
            DB.executeUpdate("UPDATE `mp_leaderboards` SET `conditions` = ? WHERE `plotId` = ?",
                    Utilities.stringArrayToString(leaderboard.getConditions()), plotId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean leaderboardExists(int plotId) {
        try {
            return DB.getFirstRow("SELECT * FROM `mp_leaderboards` WHERE `plotId` = ?", plotId) != null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeLeaderboard(int plotId) {
        try {
            DB.executeUpdate("DELETE FROM `mp_leaderboards` WHERE `plotId` = ?", plotId);
            DB.executeUpdate("DELETE FROM `mp_standings` WHERE `plotId` = ?", plotId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void updatePlayerStanding(int plotId, UUID playerUUID, int count) {
        try {
            if (DB.getFirstRow("SELECT * FROM `mp_standings` WHERE `plotId` = ? AND `playerUUID` = ?", plotId, playerUUID.toString()) != null) {
                DB.executeUpdate("UPDATE `mp_standings` SET `count` = ? WHERE `plotId` = ? AND `playerUUID` = ?", count, plotId, playerUUID.toString());
            } else {
                DB.executeUpdate("INSERT INTO `mp_standings` (`plotId`, `playerUUID`, `count`) VALUES (?, ?, ?)", plotId, playerUUID.toString(), count);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void eraseLeaderboard(Plot plot) {
        try {
            Metropolis.plotStandings.remove(plot);
            DB.executeUpdate("DELETE * FROM `mp_standings` WHERE `plotId` = ?", plot.getPlotId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int getTotalStanding(Plot plot) {
        return Metropolis.plotStandings.get(plot).stream().mapToInt(Standing::getCount).sum();
    }

    public static int getStandingEntries(Plot plot) {
        return Metropolis.plotStandings.get(plot).size();
    }

    public static void storeStandings(List<Standing> standings) {
        for (Standing standing : standings) {
            try {
                if (DB.getFirstRow("SELECT * FROM `mp_standings` WHERE `plotId` = ? AND `playerUUID` = ?", standing.getPlotId(), standing.getPlayerUUID().toString()) != null) {
                    DB.executeUpdate("UPDATE `mp_standings` SET `count` = ? WHERE `plotId` = ? AND `playerUUID` = ?", standing.getCount(), standing.getPlotId(), standing.getPlayerUUID().toString());
                } else {
                    DB.executeUpdate("INSERT INTO `mp_standings` (`plotId`, `playerUUID`, `count`) VALUES (?, ?, ?)", standing.getPlotId(), standing.getPlayerUUID().toString(), standing.getCount());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
