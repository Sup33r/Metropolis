package live.supeer.metropolis.utils;

import live.supeer.metropolis.Metropolis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.Arrays;

public class LocationUtil {
    public static Metropolis plugin;

    public static String formatLocation(Location location) {
        return "(["
                + location.getWorld().getName()
                + "]"
                + location.getBlockX()
                + ", "
                + location.getBlockY()
                + ", "
                + location.getBlockZ()
                + ")";
    }

    public static String formatChunk(String world, int x, int z) {
        return "([" + world + "]" + x + ", " + z + ")";
    }

    public static String locationToString(Location location) {
        if (location == null) {
            return null;
        }

        return location.getWorld().getName()
                + " "
                + location.getX()
                + " "
                + location.getY()
                + " "
                + location.getZ()
                + " "
                + location.getYaw()
                + " "
                + location.getPitch();
    }

    public static Location stringToLocation(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }

        String[] split = string.split(" ");
        return new Location(
                Bukkit.getWorld(split[0]),
                Double.parseDouble(split[1]),
                Double.parseDouble(split[2]),
                Double.parseDouble(split[3]),
                Float.parseFloat(split[4]),
                Float.parseFloat(split[5]));
    }

    public static String parsePoints(Location[] locations) {
        if (locations == null) {
            return null;
        }

        StringBuilder points = new StringBuilder();

        for (Location location : locations) {
            String test = "(" + location.getBlockX() + ", " + "y" + ", " + location.getBlockZ() + ")";
            if (Arrays.asList(locations).indexOf(location) == locations.length - 1) {
                points.append(test);
                break;
            }
            points.append(test).append(",");
        }
        return points.substring(0, points.length() - 1);
    }

    public static String polygonToString(Polygon polygon) {
        StringBuilder string = new StringBuilder();
        for (Coordinate coordinate : polygon.getCoordinates()) {
            string.append((int) coordinate.x).append(" ").append((int) coordinate.y).append(" ");
        }
        return string.toString();
    }

    public static Polygon stringToPolygon(String string) {
        String[] split = string.split(" ");
        Coordinate[] coordinates = new Coordinate[split.length / 2];
        for (int i = 0; i < split.length; i += 2) {
            coordinates[i / 2] = new Coordinate(Double.parseDouble(split[i]), Double.parseDouble(split[i + 1]));
        }
        return new GeometryFactory().createPolygon(coordinates);
    }

    public static Polygon createPolygonFromLocations(Location[] locations, GeometryFactory geometryFactory) {
        Coordinate[] coordinates = new Coordinate[locations.length + 1];
        for (int i = 0; i < locations.length; i++) {
            Location loc = locations[i];
            coordinates[i] = new Coordinate(loc.getX(), loc.getZ());
        }
        // Close the polygon by repeating the first coordinate at the end
        coordinates[locations.length] = coordinates[0];
        return geometryFactory.createPolygon(coordinates);
    }
}
