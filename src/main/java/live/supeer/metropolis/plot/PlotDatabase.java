package live.supeer.metropolis.plot;

import co.aikar.idb.DB;
import live.supeer.metropolis.Database;
import live.supeer.metropolis.Utilities;
import live.supeer.metropolis.city.City;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.awt.*;

public class PlotDatabase {

    public static Plot createPlot(
            Player player, Location[] plotPoints, String plotName, City city, int minY, int maxY) {
        if (plotName == null) {
            int plotAmount = getPlotAmount() + 1;
            plotName = "Tomt #" + plotAmount;
        }
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
                    "INSERT INTO `mp_plots` (`cityId`, `cityName`, `plotName`, `plotOwner`, `plotOwnerUUID`, `plotPoints`, `plotYMin`, `plotYMax`, `plotPermsMembers`, `plotPermsOutsiders`, `plotCenter`, `plotCreationDate`) VALUES ("
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
                            + Utilities.getTimestamp()
                            + ");");
            Plot plot =
                    new Plot(
                            DB.getFirstRow(
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
            for (Plot plot : city.getCityPlots()) {
                Polygon existingPlotPolygon = new Polygon();
                int minY = plot.getPlotYMin();
                int maxY = plot.getPlotYMax();
                for (Location plotPoint : plot.getPlotPoints()) {
                    existingPlotPolygon.addPoint(plotPoint.getBlockX(), plotPoint.getBlockZ());
                }
                if (polygon.intersects(existingPlotPolygon.getBounds()) && yMin <= maxY && yMax >= minY) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
