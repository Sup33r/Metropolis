package live.supeer.metropolis.plot;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import live.supeer.metropolis.Database;
import live.supeer.metropolis.utils.Utilities;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.utils.DateUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.Point;

import java.awt.*;
import java.util.List;

public class PlotDatabase {

    public static Plot createPlot(Player player, Location[] plotPoints, String plotName, City city, int minY, int maxY) {
        if (plotName == null) {
            int plotAmount = getPlotAmount() + 1;
            plotName = "Tomt #" + plotAmount;
        }

        GeometryFactory geometryFactory = new GeometryFactory();
        Coordinate[] coordinates = new Coordinate[plotPoints.length + 1];
        for (int i = 0; i < plotPoints.length; i++) {
            coordinates[i] = new Coordinate(plotPoints[i].getBlockX(), plotPoints[i].getBlockZ());
        }
        coordinates[plotPoints.length] = coordinates[0];
        Polygon plotPolygon = geometryFactory.createPolygon(coordinates);

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
        Location plotCenter = new Location(plotPoints[0].getWorld(), centerX, player.getWorld().getHighestBlockYAt(centerX, centerZ) + 1, centerZ);
        try {
            DB.executeUpdate(
                    "INSERT INTO `mp_plots` (`cityId`, `cityName`, `plotName`, `plotOwner`, `plotOwnerUUID`, `plotPoints`, `plotYMin`, `plotYMax`, `plotPermsMembers`, `plotPermsOutsiders`, `plotCenter`, `plotCreationDate`, `plotBoundary`) VALUES ("
                            + city.getCityID()
                            + ", "
                            + Database.sqlString(city.getCityName())
                            + ", "
                            + Database.sqlString(plotName)
                            + ", "
                            + Database.sqlString(player.getName())
                            + ", "
                            + Database.sqlString(player.getUniqueId().toString())
                            + ", "
                            + Database.sqlString(Utilities.polygonToString(plotPoints))
                            + ", "
                            + minY
                            + ", "
                            + maxY
                            + ", "
                            + "'gt'"
                            + ", "
                            + "'gt'"
                            + ", "
                            + Database.sqlString(Utilities.locationToString(plotCenter))
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
            DB.executeUpdate("DELETE FROM `mp_plots` WHERE `plotId` = " + plot.getPlotID() + ";");
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

    public static boolean intersectsExistingPlot(Polygon polygon, int yMin, int yMax, City city) {
        try {
            String polygonWKT = polygon.toText();
            List<DbRow> results = DB.getResults(
                    "SELECT * FROM `mp_plots` WHERE ST_Intersects(`plotBoundary`, ST_GeomFromText(" + Database.sqlString(polygonWKT) + ")) AND `cityId` = " + city.getCityID() + " AND `plotYMin` <= " + yMax + " AND `plotYMax` >= " + yMin + ";");
            return !results.isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Plot[] intersectingPlots(Polygon polygon, int yMin, int yMax, City city) {
        try {
            String polygonWKT = polygon.toText();
            List<DbRow> results = DB.getResults(
                    "SELECT * FROM `mp_plots` WHERE ST_Intersects(`plotBoundary`, ST_GeomFromText(" + Database.sqlString(polygonWKT) + ")) AND `cityId` = " + city.getCityID() + " AND `plotYMin` <= " + yMax + " AND `plotYMax` >= " + yMin + ";");
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

    public static Plot getPlotAtLocation(Location location) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Point point = geometryFactory.createPoint(new Coordinate(location.getX(), location.getZ()));

        try {
            DbRow result = DB.getFirstRow(
                    "SELECT * FROM `mp_plots` WHERE ST_Intersects(`plotBoundary`, ST_GeomFromText(?))",
                    point.toText()
            );

            if (result != null) {
                return new Plot(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
