package live.supeer.metropolis.utils;

import fr.mrmicky.fastboard.FastBoard;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.plot.Plot;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.awt.*;
import java.text.NumberFormat;
import java.util.*;


public class Utilities {
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

    public static String formattedMoney(Integer money) {
        NumberFormat formatter = NumberFormat.getInstance(Locale.US);
        formatter.setGroupingUsed(true);
        return formatter.format(money).replace(",", " ");
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



    public static boolean isCloseToOtherCity(Player player, Location location, String type) {
        int centerZ = location.getChunk().getZ();
        int centerX = location.getChunk().getX();

        for (int x = centerX - 13 / 2; x <= centerX + 12 / 2; x++) {
            for (int z = centerZ - 12 / 2; z <= centerZ + 12 / 2; z++) {
                Location chunkLocation = new Location(location.getWorld(), x * 16, 0, z * 16);
                if (CityDatabase.hasClaim(x, z, location.getWorld())) {
                    if (type.equals("newcity")) {
                        return true;
                    }
                    if (!Objects.equals(
                            Objects.requireNonNull(CityDatabase.getClaim(chunkLocation)).getCityName(),
                            HCDatabase.getHomeCityToCityname(player.getUniqueId().toString()))) {
                        return true;
                    }
                }
            }
        }
        return false;
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

    public static String parseFlagChange(char[] flagsOriginal, String change) {
        if (flagsOriginal == null) {
            flagsOriginal = new char[0];
        }
        String flagsRaw = new String(flagsOriginal);

        boolean isAdding = true;

        for (int i = 0; i < change.length(); i++) {
            char currentChar = change.charAt(i);

            // the first character must be either a + or a -
            if (i == 0 && currentChar != '+' && currentChar != '-') {
                return null;
            }

            if (flagsRaw.isEmpty() && currentChar == '-'
                    || flagsRaw.isEmpty() && currentChar == '+') {
                return null;
            }

            if (currentChar == '+') {
                isAdding = true;
                continue;
            } else if (currentChar == '-') {
                isAdding = false;
                continue;
            }

            if (isValidFlag(currentChar)) {
                return null;
            }

            flagsRaw =
                    isAdding ? flagsRaw + currentChar : flagsRaw.replace(String.valueOf(currentChar), "");
        }

        StringBuilder flagsNew = new StringBuilder();

        for (char flag : flagsRaw.toCharArray()) {
            boolean exists = false;

            for (int j = 0; j < flagsNew.length(); j++) {
                if (flagsNew.charAt(j) == flag) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                flagsNew.append(flag);
            }
        }

        char[] flagsNewArray = flagsNew.toString().toCharArray();

        Arrays.sort(flagsOriginal);
        Arrays.sort(flagsNewArray);

        flagsNew = new StringBuilder(new String(flagsNewArray));

        // don't change if there's nothing to change
        if (flagsNew.toString().equals(new String(flagsOriginal))) {
            return null;
        }

        return flagsNew.toString();
    }

    private static boolean isValidFlag(char currentChar) {
        // a = animals, c = conference (meetings), i = items, l = locked, m = monsters, p = pvp, x =
        // experience
        return currentChar != 'a'
                && currentChar != 'c'
                && currentChar != 'i'
                && currentChar != 'l'
                && currentChar != 'm'
                && currentChar != 'p'
                && currentChar != 'x';
    }

    public static String parsePermChange(
            char[] flagsOriginal, String change, Player player, String flagType) {
        if (flagsOriginal == null) {
            flagsOriginal = new char[0];
        }
        String flagsRaw = new String(flagsOriginal);
        change = change.replace("*", "abcefghjrstv");

        boolean isAdding = true;

        for (int i = 0; i < change.length(); i++) {
            char currentChar = change.charAt(i);

            // the first character must be either a + or a -
            if (i == 0 && currentChar != '+' && currentChar != '-') {
                return null;
            }

            if (flagsRaw.isEmpty() && currentChar == '-'
                    || flagsRaw.isEmpty() && currentChar == '+') {
                return null;
            }

            if (currentChar == '+') {
                isAdding = true;
                continue;
            } else if (currentChar == '-') {
                isAdding = false;
                continue;
            }

            if (isValidFlag(currentChar)) {
                if (flagType.equals("plot")) {
                    plugin.sendMessage(player, "messages.error.plot.perm.notFound");
                } else {
                    plugin.sendMessage(player, "messages.error.city.perm.notFound");
                }
                return null;
            }

            flagsRaw =
                    isAdding ? flagsRaw + currentChar : flagsRaw.replace(String.valueOf(currentChar), "");
        }

        StringBuilder flagsNew = new StringBuilder();

        for (char flag : flagsRaw.toCharArray()) {
            boolean exists = false;

            for (int j = 0; j < flagsNew.length(); j++) {
                if (flagsNew.charAt(j) == flag) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                flagsNew.append(flag);
            }
        }

        char[] flagsNewArray = flagsNew.toString().toCharArray();

        Arrays.sort(flagsOriginal);
        Arrays.sort(flagsNewArray);

        flagsNew = new StringBuilder(new String(flagsNewArray));

        // don't change if there's nothing to change
        if (flagsNew.toString().equals(new String(flagsOriginal))) {
            if (flagType.equals("plot")) {
                plugin.sendMessage(player, "messages.error.plot.perm.noChange");
            } else {
                plugin.sendMessage(player, "messages.error.city.perm.noChange");
            }
            return null;
        }

        return flagsNew.toString();
    }

    public static String parsePerms(String flags, String flagType, Player player) {
        if (flags == null) {
            flags = "";
        }

        flags = flags.replace("*", "abcefghjrstv");

        StringBuilder flagsNew = new StringBuilder();

        for (char flag : flags.toCharArray()) {
            if (isValidPerm(flag)) {
                if (flagType.equals("plot")) {
                    plugin.sendMessage(player, "messages.error.plot.perm.notFound");
                } else {
                    plugin.sendMessage(player, "messages.error.city.perm.notFound");
                }
                return null;
            }

            boolean exists = false;

            for (int j = 0; j < flagsNew.length(); j++) {
                if (flagsNew.charAt(j) == flag) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                flagsNew.append(flag);
            }
        }

        char[] flagsNewArray = flagsNew.toString().toCharArray();

        Arrays.sort(flagsNewArray);

        flagsNew = new StringBuilder(new String(flagsNewArray));

        return flagsNew.toString();
    }

    private static boolean isValidPerm(char currentChar) {
        return currentChar != 'a'
                && currentChar != 'b'
                && currentChar != 'c'
                && currentChar != 'e'
                && currentChar != 'f'
                && currentChar != 'g'
                && currentChar != 'h'
                && currentChar != 'j'
                && currentChar != 'r'
                && currentChar != 's'
                && currentChar != 't'
                && currentChar != 'v';
    }

    public static String polygonToString(Location[] polygon) {
        StringBuilder string = new StringBuilder();
        for (Location location : polygon) {
            string.append(locationToString(location)).append(" ");
        }
        return string.toString();
    }

    public static Location[] stringToPolygon(String string) {
        String[] split = string.split(" ");
        Location[] polygon = new Location[split.length / 6];
        for (int i = 0; i < split.length; i += 6) {
            polygon[i / 6] =
                    stringToLocation(
                            split[i]
                                    + " "
                                    + split[i + 1]
                                    + " "
                                    + split[i + 2]
                                    + " "
                                    + split[i + 3]
                                    + " "
                                    + split[i + 4]
                                    + " "
                                    + split[i + 5]);
        }
        return polygon;
    }

    public static void sendCityScoreboard(Player player, City city) {
        FastBoard board = new FastBoard(player);
        int i = 0;
        if (CityDatabase.getClaim(player.getLocation()) != null) {
            board.updateTitle("§a             §l" + city.getCityName() + "§r             ");
            board.updateLine(i, " ");
            i = i + 1;
            for (Plot plot : city.getCityPlots()) {
                Polygon polygon = new Polygon();
                int yMin = plot.getPlotYMin();
                int yMax = plot.getPlotYMax();
                for (Location loc : plot.getPlotPoints()) {
                    polygon.addPoint(loc.getBlockX(), loc.getBlockZ());
                }
                if (player.getLocation().getBlockY() >= yMin && player.getLocation().getBlockY() <= yMax) {
                    if (polygon.contains(
                            player.getLocation().getBlockX(), player.getLocation().getBlockZ())) {
                        if (plot.isKMarked()) {
                            board.updateLine(i, plugin.getMessage("messages.city.scoreboard.placeK"));
                        } else {
                            board.updateLine(i, plugin.getMessage("messages.city.scoreboard.place"));
                        }
                        board.updateLine(i + 1, "§a" + plot.getPlotName());
                        i = i + 2;
                        board.updateLine(i, " ");
                        i = i + 1;
                        if (plot.getPlotType() != null) {
                            board.updateLine(i, plugin.getMessage("messages.city.scoreboard.type"));
                            String type = plot.getPlotType();
                            if (type.equals("church")) {
                                type = "Kyrka";
                            }
                            if (type.equals("farm")) {
                                type = "Farm";
                            }
                            if (type.equals("shop")) {
                                type = "Affär";
                            }
                            if (type.equals("vacation")) {
                                type = "Ferietomt";
                            }
                            if (type.equals("jail")) {
                                type = "Fängelse";
                            }
                            board.updateLine(i + 1, "§a" + type);
                            board.updateLine(i + 2, " ");
                            i = i + 3;
                        }
                        if (plot.getPlotOwner() != null) {
                            board.updateLine(i, plugin.getMessage("messages.city.scoreboard.owner"));
                            board.updateLine(i + 1, "§a" + plot.getPlotOwner());
                            board.updateLine(i + 2, " ");
                            i = i + 3;
                        }
                        if (plot.isForSale()) {
                            board.updateLine(i, plugin.getMessage("messages.city.scoreboard.price"));
                            board.updateLine(
                                    i + 1, "§a" + Utilities.formattedMoney(plot.getPlotPrice()) + " minemynt");
                            if (plot.getPlotRent() != 0) {
                                board.updateLine(
                                        i + 2, "§aTR: " + Utilities.formattedMoney(plot.getPlotRent()) + " minemynt");
                            }
                        }
                        if (board.getLine(board.size() - 1).equals(" ")) {
                            board.removeLine(board.size() - 1);
                        }
                        return;
                    }
                }
            }

            board.updateLine(i, plugin.getMessage("messages.city.scoreboard.members"));
            i = i + 1;
            board.updateLine(i, "§a" + city.getCityMembers().size());
            i = i + 1;
            board.updateLine(i, " ");
            i = i + 1;
            board.updateLine(i, plugin.getMessage("messages.city.scoreboard.plots"));
            i = i + 1;
            board.updateLine(i, "§a" + city.getCityPlots().size());

        } else {
            board.updateTitle(plugin.getMessage("messages.city.scoreboard.nature"));
            board.updateLine(0, plugin.getMessage("messages.city.scoreboard.pvp_on"));
        }
    }

    public static void sendNatureScoreboard(Player player) {
        FastBoard board = new FastBoard(player);
        board.updateTitle(plugin.getMessage("messages.city.scoreboard.nature"));
        board.updateLine(0, plugin.getMessage("messages.city.scoreboard.pvp_on"));
    }

    public static org.locationtech.jts.geom.Polygon createPolygonFromLocations(Location[] locations, GeometryFactory geometryFactory) {
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
