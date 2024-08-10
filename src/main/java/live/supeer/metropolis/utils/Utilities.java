package live.supeer.metropolis.utils;

import fr.mrmicky.fastboard.FastBoard;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.city.*;
import live.supeer.metropolis.command.CommandCity;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.plot.PlotDatabase;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.text.NumberFormat;
import java.util.*;


public class Utilities {
    public static Metropolis plugin;


    public static String formattedMoney(Integer money) {
        NumberFormat formatter = NumberFormat.getInstance(Locale.US);
        formatter.setGroupingUsed(true);
        return formatter.format(money).replace(",", " ");
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
                            Objects.requireNonNull(CityDatabase.getClaim(chunkLocation)).getCity(),
                            HCDatabase.getHomeCityToCity(player.getUniqueId().toString()))) {
                        return true;
                    }
                }
            }
        }
        return false;
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

            if (currentChar == '+') {
                isAdding = true;
                continue;
            } else if (currentChar == '-') {
                isAdding = false;
                continue;
            }

            // check if the flag is valid
            if (isValidPerm(currentChar)) {
                if (flagType.equals("plot")) {
                    plugin.sendMessage(player, "messages.error.plot.perm.notFound");
                } else {
                    plugin.sendMessage(player, "messages.error.city.perm.notFound");
                }
                return null;
            }

            flagsRaw = isAdding ? flagsRaw + currentChar : flagsRaw.replace(String.valueOf(currentChar), "");
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

    private static String getFormattedTitle(String cityName) {
        int nameLength = cityName.length();
        int spaces = Math.max(5, 15 - (nameLength - 4));
        String spaceString = " ".repeat(spaces);
        return "§a" + spaceString + "§l" + cityName + "§r" + spaceString;
    }

    public static void sendCityScoreboard(Player player, City city, Plot plot) {
        District district = Metropolis.playerInDistrict.get(player.getUniqueId());
        FastBoard board = new FastBoard(player);
        int i = 0;
        board.updateTitle(getFormattedTitle(city.getCityName()));
        board.updateLine(i, " ");
        i = i + 1;
        if (district != null) {
            board.updateLine(i, plugin.getMessage("messages.city.scoreboard.district"));
            board.updateLine(i + 1, "§a" + district.getDistrictName());
            i = i + 2;
            board.updateLine(i, " ");
            i = i + 1;
        }
        if (plot != null) {
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
                    type = plugin.getMessage("messages.plot.type.church");
                }
                if (type.equals("farm")) {
                    type = plugin.getMessage("messages.plot.type.farm");
                }
                if (type.equals("shop")) {
                    type = plugin.getMessage("messages.plot.type.shop");
                }
                if (type.equals("vacation")) {
                    type = plugin.getMessage("messages.plot.type.vacation");
                }
                if (type.equals("jail")) {
                    type = plugin.getMessage("messages.plot.type.jail");
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
                board.updateLine(i + 1, "§a" + Utilities.formattedMoney(plot.getPlotPrice()) + " minemynt");
                if (plot.getPlotRent() != 0) {
                    board.updateLine(i + 2, "§aTR: " + Utilities.formattedMoney(plot.getPlotRent()) + " minemynt");
                }
            }
            if (board.getLine(board.size() - 1).equals(" ")) {
                board.removeLine(board.size() - 1);
            }
        } else {
            board.updateLine(i, plugin.getMessage("messages.city.scoreboard.members"));
            i = i + 1;
            board.updateLine(i, "§a" + city.getCityMembers().size());
            i = i + 1;
            board.updateLine(i, " ");
            i = i + 1;
            board.updateLine(i, plugin.getMessage("messages.city.scoreboard.plots"));
            i = i + 1;
            board.updateLine(i, "§a" + city.getCityPlots().size());
        }
    }

    public static void sendNatureScoreboard(Player player) {
        FastBoard board = new FastBoard(player);
        board.updateTitle(plugin.getMessage("messages.city.scoreboard.nature"));
        board.updateLine(0, plugin.getMessage("messages.city.scoreboard.pvp_on"));
    }

    public static void sendScoreboard(Player player) {
        if (!Metropolis.playerInCity.containsKey(player.getUniqueId())) {
            sendNatureScoreboard(player);
            return;
        }
        City city = Metropolis.playerInCity.get(player.getUniqueId());
        Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
        District district = Metropolis.playerInDistrict.get(player.getUniqueId());
        sendCityScoreboard(player, city, plot);
    }
    public static City hasCityPermissions(Player player, String permission, Role targetRole) {
        if (!player.hasPermission(permission)) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return null;
        }
        City city = HCDatabase.getHomeCityToCity(player.getUniqueId().toString());
        if (city == null) {
            plugin.sendMessage(player, "messages.error.missing.homeCity");
            return null;
        }
        if (targetRole != null) {
            Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
            if (role == null) {
                plugin.sendMessage(player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                return null;
            }
            if (role.getPermissionLevel() < targetRole.getPermissionLevel()) {
                plugin.sendMessage(player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                return null;
            }
        }
        return city;
    }
    public static Plot hasPlotPermissions(Player player, String permission, Role targetRole, boolean isOwner) {
        if (permission != null && !player.hasPermission(permission)) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return null;
        }
        Plot plot = PlotDatabase.getPlotAtLocation(player.getLocation());
        if (plot == null) {
            plugin.sendMessage(player, "messages.error.plot.notInPlot");
            return null;
        }
        Role role = CityDatabase.getCityRole(plot.getCity(), player.getUniqueId().toString());
        if (isOwner) {
            if (plot.getPlotOwner().equals(player.getUniqueId().toString())) {
                return plot;
            }
            if (role != null && targetRole != null) {
                if (role.getPermissionLevel() > targetRole.getPermissionLevel()) {
                    return plot;
                }
            }
            plugin.sendMessage(player, "messages.error.plot.permissionDenied");
            return null;
        } else {
            if (role == null) {
                plugin.sendMessage(player, "messages.error.plot.permissionDenied");
                return null;
            }
            if (role.getPermissionLevel() < targetRole.getPermissionLevel()) {
                plugin.sendMessage(player, "messages.error.plot.permissionDenied");
                return null;
            }
        }
        return plot;
    }

    public static boolean cityCanClaim(City city) {
        return city.getCityClaims().size() < city.getCityMembers().size() * 20 + city.getBonusClaims();
    }

    public static String[][] generateAsciiMap(Player player, int centerX, int centerZ, City playerCity) {
        String[][] map = new String[10][29];

        for (int z = 0; z < 10; z++) {
            for (int x = 0; x < 29; x++) {
                int chunkX = centerX - 14 + x;
                int chunkZ = centerZ - 5 + z;

                if (x == 14 && z == 5) {
                    map[z][x] = "§0■"; // Player position (black)
                } else {
                    City chunkCity = CityDatabase.getCityByClaim(new Location(player.getWorld(), chunkX << 4, 0, chunkZ << 4));
                    if (chunkCity == null) {
                        map[z][x] = "§f■"; // Unclaimed (white)
                    } else if (chunkCity.equals(playerCity)) {
                        map[z][x] = "§a■"; // Player's city (green)
                    } else {
                        map[z][x] = "§c■"; // Other city (red)
                    }
                }
            }
        }

        return map;
    }

    public static void sendMapToPlayer(Player player, String[][] asciiMap, City city) {
        CommandCity.plugin.sendMessage(player, "messages.city.map.header", "%cityname%", city != null ? city.getCityName() : CommandCity.plugin.getMessage("messages.words.wilderness"));
        if (city == null) {
            CommandCity.plugin.sendMessage(player, "messages.city.map.legend.wilderness");
        } else {
            CommandCity.plugin.sendMessage(player, "messages.city.map.legend.city", "%cityname%", city.getCityName());
        }

        StringBuilder mapBuilder = new StringBuilder();
        for (String[] row : asciiMap) {
            for (String cell : row) {
                mapBuilder.append(cell);
            }
            mapBuilder.append("\n");
        }

        player.sendMessage(mapBuilder.toString());
    }

    //make a method to convert a string of offline players to a list of offline players
    public static List<OfflinePlayer> stringToOfflinePlayerList(String playerString) {
        List<OfflinePlayer> players = new ArrayList<>();
        if (playerString == null || playerString.isEmpty()) {
            return players;
        }
        String[] playerArray = playerString.split(",");
        for (String player : playerArray) {
            players.add(Bukkit.getOfflinePlayer(UUID.fromString(player)));
        }
        return players;
    }


    public static String offlinePlayerListToString(List<OfflinePlayer> players) {
        StringBuilder playerString = new StringBuilder();
        for (OfflinePlayer player : players) {
            playerString.append(player.getUniqueId().toString()).append(",");
        }
        return playerString.toString();
    }

    public static List<City> stringToCityList(String cityString) {
        List<City> cities = new ArrayList<>();
        if (cityString == null || cityString.isEmpty()) {
            return cities;
        }
        String[] cityArray = cityString.split(",");
        for (String city : cityArray) {
            cities.add(CityDatabase.getCity(Integer.parseInt(city)).get());
        }
        return cities;
    }

    public static String cityListToString(List<City> cities) {
        StringBuilder cityString = new StringBuilder();
        for (City city : cities) {
            cityString.append(city.getCityId()).append(",");
        }
        return cityString.toString();
    }

    public static boolean containsOnlyCompletePlots(Polygon polygon, int yMin, int yMax, City city, World world) {
        Plot[] intersectingPlots = PlotDatabase.intersectingPlots(polygon, yMin, yMax, city, world);

        if (intersectingPlots == null || intersectingPlots.length == 0) {
            // No intersecting plots, so the condition is satisfied
            return true;
        }

        GeometryFactory geometryFactory = new GeometryFactory();

        for (Plot plot : intersectingPlots) {
            Polygon plotPolygon = plot.getPlotPoints();

            if (!polygon.contains(plotPolygon)) {
                // If any plot is not completely contained, return false
                return false;
            }
        }

        // All intersecting plots are completely contained
        return true;
    }

    public static boolean cannotClaimOrCreateCity(Location location, City city) {
        int minSpawnDistance;
        int minChunkDistance;

        if (city == null) {
            minSpawnDistance = Metropolis.configuration.getMinSpawnDistance();
            minChunkDistance = Metropolis.configuration.getMinChunkDistance();
        } else {
            minSpawnDistance = city.getMinSpawnDistance();
            minChunkDistance = city.getMinChunkDistance();
        }

        Location chunkCenter = new Location(location.getWorld(),
                location.getChunk().getX() * 16 + 8,
                location.getY(),
                location.getChunk().getZ() * 16 + 8);

        List<CityDistance> nearbyCities = CityDatabase.getCitiesWithinRadius(chunkCenter, Math.max(minChunkDistance, minSpawnDistance));

        for (CityDistance cityDistance : nearbyCities) {
            City existingCity = cityDistance.getCity();
            int distance = cityDistance.getDistance();

            if (city == null) {
                if (distance < minSpawnDistance) {
                    return true;
                }

                if (distance < minChunkDistance) {
                    return true;
                }
            } else {
                boolean isTwin = existingCity.getTwinCities().contains(city);

                if (!isTwin) {
                    if (distance < minSpawnDistance) {
                        return true;
                    }

                    if (distance < minChunkDistance) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static String taxPayedBy(String role) {
        switch (role) {
            case "mayor":
                return plugin.getMessage("messages.city.cityInfo.roles.mayor") + ", " + plugin.getMessage("messages.city.cityInfo.roles.vicemayor") + ", " + plugin.getMessage("messages.city.cityInfo.roles.assistant") + ", " + plugin.getMessage("messages.city.cityInfo.roles.inviter") + " " + plugin.getMessage("messages.words.and") + " " + plugin.getMessage("messages.city.cityInfo.roles.member");
            case "vicemayor":
                return plugin.getMessage("messages.city.cityInfo.roles.vicemayor") + ", " + plugin.getMessage("messages.city.cityInfo.roles.assistant") + ", " + plugin.getMessage("messages.city.cityInfo.roles.inviter") + " " + plugin.getMessage("messages.words.and") + " " + plugin.getMessage("messages.city.cityInfo.roles.member");
            case "assistant":
                return plugin.getMessage("messages.city.cityInfo.roles.assistant") + ", " + plugin.getMessage("messages.city.cityInfo.roles.inviter") + " " + plugin.getMessage("messages.words.and") + " " + plugin.getMessage("messages.city.cityInfo.roles.member");
            case "inviter":
                return plugin.getMessage("messages.city.cityInfo.roles.inviter") + " " + plugin.getMessage("messages.words.and") + " " + plugin.getMessage("messages.city.cityInfo.roles.member");
            case "none":
                return plugin.getMessage("messages.city.cityInfo.roles.none");
            default:
                return plugin.getMessage("messages.city.cityInfo.roles.member");
        }
    }

    public static String formatStringList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        int size = items.size();

        if (size == 1) {
            return items.get(0);
        }

        if (size == 2) {
            return String.format("%s & %s", items.get(0), items.get(1));
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < size - 1; i++) {
            result.append(items.get(i));
            if (i < size - 2) {
                result.append(", ");
            } else {
                result.append(" & ");
            }
        }
        result.append(items.get(size - 1));

        return result.toString();
    }

}
