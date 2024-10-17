package live.supeer.metropolis.utils;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.AnvilGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import fr.mrmicky.fastboard.FastBoard;
import live.supeer.apied.Apied;
import live.supeer.apied.ApiedAPI;
import live.supeer.apied.MPlayer;
import live.supeer.metropolis.Leaderboard;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.MetropolisListener;
import live.supeer.metropolis.Standing;
import live.supeer.metropolis.city.*;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.plot.PlotDatabase;
import live.supeer.metropolis.plot.PlotPerms;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;


public class Utilities {
    public static String formattedMoney(Integer money) {
        NumberFormat formatter = NumberFormat.getInstance(Locale.US);
        formatter.setGroupingUsed(true);
        return formatter.format(money).replace(",", " ");
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

            // Only return null if attempting to remove from empty set
            if (flagsRaw.isEmpty() && currentChar == '-') {
                return null;
            }

            if (currentChar == '+') {
                isAdding = true;
                continue;
            } else if (currentChar == '-') {
                isAdding = false;
                continue;
            }

            flagsRaw = isAdding ? flagsRaw + currentChar : flagsRaw.replace(String.valueOf(currentChar), "");
        }

        // Remove duplicates
        StringBuilder flagsNew = new StringBuilder();
        for (char flag : flagsRaw.toCharArray()) {
            if (flagsNew.indexOf(String.valueOf(flag)) == -1) {
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

    public static String parsePermChange(
            char[] flagsOriginal, String change, Player player, String flagType) {
        if (flagsOriginal == null) {
            flagsOriginal = new char[0];
        }
        String flagsRaw = new String(flagsOriginal);

        change = change.replace("*", "abcefghjkrstv");
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
                    Metropolis.sendMessage(player, "messages.error.plot.perm.notFound");
                } else {
                    Metropolis.sendMessage(player, "messages.error.city.perm.notFound");
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
                Metropolis.sendMessage(player, "messages.error.plot.perm.noChange");
            } else {
                Metropolis.sendMessage(player, "messages.error.city.perm.noChange");
            }
            return null;
        }

        return flagsNew.toString();
    }

    public static String parsePerms(String flags, String flagType, Player player) {
        if (flags == null) {
            flags = "";
        }

        flags = flags.replace("*", "abcefghjkrstv");

        StringBuilder flagsNew = new StringBuilder();

        for (char flag : flags.toCharArray()) {
            if (isValidPerm(flag)) {
                if (flagType.equals("plot")) {
                    Metropolis.sendMessage(player, "messages.error.plot.perm.notFound");
                } else {
                    Metropolis.sendMessage(player, "messages.error.city.perm.notFound");
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
                && currentChar != 'k'
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

        FastBoard board = MetropolisListener.scoreboards.get(player.getUniqueId());
        board.updateLines();
        int i = 0;
        board.updateTitle(getFormattedTitle(city.getCityName()));
        board.updateLine(i, " ");
        i = i + 1;

        if (district != null) {
            board.updateLine(i, Metropolis.getMessage("messages.city.scoreboard.district"));
            board.updateLine(i + 1, "§a" + district.getDistrictName());
            i = i + 2;
            board.updateLine(i, " ");
            i = i + 1;
        }

        if (plot != null) {
            if (plot.hasFlag('c')) {
                board.updateLine(i, Metropolis.getMessage("messages.city.scoreboard.meeting"));
                i = i + 1;
            }
            if (plot.isKMarked()) {
                board.updateLine(i, Metropolis.getMessage("messages.city.scoreboard.placeK"));
            } else {
                board.updateLine(i, Metropolis.getMessage("messages.city.scoreboard.place"));
            }
            board.updateLine(i + 1, "§a" + plot.getPlotName());
            i = i + 2;
            board.updateLine(i, " ");
            i = i + 1;

            if (plot.getPlotType() != null) {
                board.updateLine(i, Metropolis.getMessage("messages.city.scoreboard.type"));
                String type = plot.getPlotType();
                switch (type) {
                    case "church" -> type = Metropolis.getMessage("messages.plot.type.church");
                    case "farm" -> type = Metropolis.getMessage("messages.plot.type.farm");
                    case "shop" -> type = Metropolis.getMessage("messages.plot.type.shop");
                    case "vacation" -> type = Metropolis.getMessage("messages.plot.type.vacation");
                    case "jail" -> type = Metropolis.getMessage("messages.plot.type.jail");
                }
                board.updateLine(i + 1, "§a" + type);
                board.updateLine(i + 2, " ");
                i = i + 3;
            }
            if (plot.getPlotOwnerUUID() != null) {
                MPlayer mOwner = ApiedAPI.getPlayer(UUID.fromString(plot.getPlotOwnerUUID()));
                if (mOwner != null) {
                    board.updateLine(i, Metropolis.getMessage("messages.city.scoreboard.owner"));
                    board.updateLine(i + 1, "§a" + mOwner.getName());
                    board.updateLine(i + 2, " ");
                    i = i + 3;
                }
            }

            // Leaderboard section
            if (plot.isLeaderboardShown()) {
                Leaderboard leaderboard = Metropolis.plotLeaderboards.get(plot);
                List<Standing> topThree = Utilities.getTopThree(plot);
                board.updateLine(i, Metropolis.getMessage("messages.plot.scoreboard.leaderboard"));
                i++;
                String type = "";
                if (leaderboard.getType().equalsIgnoreCase("place") || leaderboard.getType().equalsIgnoreCase("break")) {
                    type = Metropolis.getMessage("messages.plot.scoreboard.leaderboardType.blocks");
                } else if (leaderboard.getType().equalsIgnoreCase("mobs")) {
                    type = Metropolis.getMessage("messages.plot.scoreboard.leaderboardType.mobs");
                }
                if (!topThree.isEmpty()) {
                    int rank = 1;
                    for (Standing standing : topThree) {
                        MPlayer mPlayer = ApiedAPI.getPlayer(standing.getPlayerUUID());
                        if (mPlayer == null) {
                            continue;
                        }
                        board.updateLine(i, Metropolis.getMessage("messages.plot.scoreboard.leaderboardLine", "%position%", String.valueOf(rank), "%player%", mPlayer.getName(), "%score%", formattedMoney(standing.getCount()), "%type%", type));
                        i++;
                        rank++;
                    }
                    i++; // Space after leaderboard
                } else {
                    board.updateLine(i, Metropolis.getMessage("messages.plot.scoreboard.noStandings"));
                    i++;
                }
            }
            if (plot.isForSale()) {
                board.updateLine(i, Metropolis.getMessage("messages.city.scoreboard.price"));
                board.updateLine(i + 1, "§a" + Utilities.formattedMoney(plot.getPlotPrice()) + " minemynt");
                if (plot.getPlotRent() != 0) {
                    board.updateLine(i + 2, "§aTR: " + Utilities.formattedMoney(plot.getPlotRent()) + " minemynt");
                }
            }

            if (board.getLine(board.size() - 1).equals(" ")) {
                board.removeLine(board.size() - 1);
            }
        } else {
            board.updateLine(i, Metropolis.getMessage("messages.city.scoreboard.members"));
            i = i + 1;
            board.updateLine(i, "§a" + city.getCityMembers().size());
            i = i + 1;
            board.updateLine(i, " ");
            i = i + 1;
            board.updateLine(i, Metropolis.getMessage("messages.city.scoreboard.plots"));
            i = i + 1;
            board.updateLine(i, "§a" + city.getCityPlots().size());
        }
    }

    public static void sendNatureScoreboard(Player player) {
        FastBoard board = new FastBoard(player);
        board.updateTitle(Metropolis.getMessage("messages.city.scoreboard.nature"));
        board.updateLine(0, Metropolis.getMessage("messages.city.scoreboard.pvp_on"));
    }

    public static void sendScoreboard(Player player) {
        if (!Metropolis.playerInCity.containsKey(player.getUniqueId())) {
            sendNatureScoreboard(player);
            return;
        }
        City city = Metropolis.playerInCity.get(player.getUniqueId());
        Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
        sendCityScoreboard(player, city, plot);
    }
    public static City hasCityPermissions(Player player, String permission, Role targetRole) {
        if (!player.hasPermission(permission)) {
            Metropolis.sendAccessDenied(player);
            return null;
        }
        City city = HCDatabase.getHomeCityToCity(player.getUniqueId().toString());
        if (city == null) {
            Metropolis.sendMessage(player, "messages.error.missing.homeCity");
            return null;
        }
        if (targetRole != null) {
            Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
            if (role == null) {
                Metropolis.sendMessage(player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                return null;
            }
            if (role.getPermissionLevel() < targetRole.getPermissionLevel()) {
                Metropolis.sendMessage(player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                return null;
            }
        }
        return city;
    }
    public static Plot hasPlotPermissions(Player player, String permission, Role targetRole, boolean isOwner) {
        if (permission != null && !player.hasPermission(permission)) {
            Metropolis.sendAccessDenied(player);
            return null;
        }
        Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
        if (plot == null) {
            Metropolis.sendMessage(player, "messages.error.plot.notInPlot");
            return null;
        }
        Role role = CityDatabase.getCityRole(plot.getCity(), player.getUniqueId().toString());
        if (isOwner) {
            if (plot.getPlotOwnerUUID() != null && plot.getPlotOwnerUUID().equals(player.getUniqueId().toString())) {
                return plot;
            }
            if (role != null && targetRole != null) {
                if (role.getPermissionLevel() > targetRole.getPermissionLevel()) {
                    return plot;
                }
            }
            Metropolis.sendMessage(player, "messages.error.city.permissionDenied", "%cityname%", plot.getCity().getCityName());
            return null;
        } else {
            if (role == null) {
                Metropolis.sendMessage(player, "messages.error.city.permissionDenied", "%cityname%", plot.getCity().getCityName());
                return null;
            }
            if (role.getPermissionLevel() < targetRole.getPermissionLevel()) {
                Metropolis.sendMessage(player, "messages.error.city.permissionDenied", "%cityname%", plot.getCity().getCityName());
                return null;
            }
        }
        return plot;
    }

    public static boolean cityCanClaim(City city) {
        return city.getCityClaims() < city.getCityMembers().size() * 20 + city.getBonusClaims();
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
        Metropolis.sendMessage(player, "messages.city.map.header", "%cityname%", city != null ? city.getCityName() : Metropolis.getMessage("messages.words.wilderness"));
        if (city == null) {
            Metropolis.sendMessage(player, "messages.city.map.legend.wilderness");
        } else {
            Metropolis.sendMessage(player, "messages.city.map.legend.city", "%cityname%", city.getCityName());
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

        if (intersectingPlots == null) {
            return true;
        }
        for (Plot plot : intersectingPlots) {
            Polygon plotPolygon = plot.getPlotPoints();
            if (!polygon.contains(plotPolygon)) {
                return false;
            }
        }
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
            if (existingCity == null || existingCity.equals(city)) {
                continue;
            }
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
                return Metropolis.getMessage("messages.city.cityInfo.roles.mayor") + ", " + Metropolis.getMessage("messages.city.cityInfo.roles.vicemayor") + ", " + Metropolis.getMessage("messages.city.cityInfo.roles.assistant") + ", " + Metropolis.getMessage("messages.city.cityInfo.roles.inviter") + " " + Metropolis.getMessage("messages.words.and") + " " + Metropolis.getMessage("messages.city.cityInfo.roles.member");
            case "vicemayor":
                return Metropolis.getMessage("messages.city.cityInfo.roles.vicemayor") + ", " + Metropolis.getMessage("messages.city.cityInfo.roles.assistant") + ", " + Metropolis.getMessage("messages.city.cityInfo.roles.inviter") + " " + Metropolis.getMessage("messages.words.and") + " " + Metropolis.getMessage("messages.city.cityInfo.roles.member");
            case "assistant":
                return Metropolis.getMessage("messages.city.cityInfo.roles.assistant") + ", " + Metropolis.getMessage("messages.city.cityInfo.roles.inviter") + " " + Metropolis.getMessage("messages.words.and") + " " + Metropolis.getMessage("messages.city.cityInfo.roles.member");
            case "inviter":
                return Metropolis.getMessage("messages.city.cityInfo.roles.inviter") + " " + Metropolis.getMessage("messages.words.and") + " " + Metropolis.getMessage("messages.city.cityInfo.roles.member");
            case "none":
                return Metropolis.getMessage("messages.city.cityInfo.roles.none");
            default:
                return Metropolis.getMessage("messages.city.cityInfo.roles.member");
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

    public static boolean isBlockContainer(Material material) {
        return material == Material.CHEST
                || material == Material.TRAPPED_CHEST
                || material == Material.BARREL
                || material == Material.SHULKER_BOX
                || material == Material.BLACK_SHULKER_BOX
                || material == Material.BLUE_SHULKER_BOX
                || material == Material.BROWN_SHULKER_BOX
                || material == Material.CYAN_SHULKER_BOX
                || material == Material.GRAY_SHULKER_BOX
                || material == Material.GREEN_SHULKER_BOX
                || material == Material.LIGHT_BLUE_SHULKER_BOX
                || material == Material.LIGHT_GRAY_SHULKER_BOX
                || material == Material.LIME_SHULKER_BOX
                || material == Material.MAGENTA_SHULKER_BOX
                || material == Material.ORANGE_SHULKER_BOX
                || material == Material.PINK_SHULKER_BOX
                || material == Material.PURPLE_SHULKER_BOX
                || material == Material.RED_SHULKER_BOX
                || material == Material.WHITE_SHULKER_BOX
                || material == Material.YELLOW_SHULKER_BOX;
    }

    public static String[] stringToStringArray(String string) {
        if (string == null || string.isEmpty()) {
            return new String[0];
        }
        return string.split(",");
    }

    public static String stringArrayToString(String[] strings) {
        StringBuilder string = new StringBuilder();
        for (String s : strings) {
            string.append(s).append(",");
        }
        return string.toString();
    }

    public static List<Standing> getTopThree(Plot plot) {
        List<Standing> standings = new ArrayList<>(Metropolis.plotStandings.get(plot));
        standings.sort((s1, s2) -> Integer.compare(s2.getCount(), s1.getCount()));
        return standings.stream().limit(3).collect(Collectors.toList());
    }

    public static String listConditions(List<String> conditions, boolean block) {
        StringBuilder messageBuilder = new StringBuilder();
        for (String condition : conditions) {
            if (block){
                messageBuilder.append("<dark_green><lang:block.minecraft.").append(condition).append("><green>, ");
            } else {
                messageBuilder.append("<dark_green><lang:entity.minecraft.").append(condition).append("><green>, ");
            }
        }

        String message = messageBuilder.toString();
        return message.substring(0, message.length() - 8);
    }

    public static List<UUID> stringToUUIDList(String playerString) {
        List<UUID> players = new ArrayList<>();
        if (playerString == null || playerString.isEmpty()) {
            return players;
        }
        String[] playerArray = playerString.split(",");
        for (String playerUUID : playerArray) {
            players.add(UUID.fromString(playerUUID));
        }
        return players;
    }

    public static @NotNull String formatPerms(char[] perms) {
        StringBuilder stringBuilder = new StringBuilder();
        for (char s : perms) {
            stringBuilder.append(s);
        }
        String stringPerms = "+"
                + stringBuilder.substring(
                0, stringBuilder.toString().length());
        if (stringBuilder
                .substring(0, stringBuilder.toString().length())
                .equals(" ")
                || stringBuilder
                .substring(0, stringBuilder.toString().length())
                .isEmpty()
                || stringBuilder.substring(0, stringBuilder.toString().length())
                == null) {
            stringPerms = "<italic>nada";
        }
        return stringPerms;
    }

    public static String uuidListToString(List<UUID> players) {
        StringBuilder playerString = new StringBuilder();
        for (UUID player : players) {
            playerString.append(player.toString()).append(",");
        }
        return playerString.toString();
    }

    public static boolean hasPermissionFlags(UUID uuid, City city, Plot plot, char flag) {
        if (plot != null) {
            if (city == null) {
                city = plot.getCity();
            }
            //TODO: Fixa en bättre lösning för det här, för den här quickfixen är riktigt dålig...
            if (plot.hasFlag('l')) {
                return false;
            }
            if (city.getCityMember(uuid.toString()) != null) {
                if (city.getCityMember(uuid.toString()).getCityRole().hasPermission(Role.ASSISTANT) && !plot.isKMarked()) {
                    return true;
                }
            }
            PlotPerms perms = plot.getPlayerPlotPerm(uuid.toString());
            if (perms != null) {
                if (new String(perms.getPerms()).contains(String.valueOf(flag))) {
                    return true;
                }
            }
            if (city.getCityMember(uuid.toString()) != null && plot.getPermsMembers() != null) {
                if (new String(plot.getPermsMembers()).contains(String.valueOf(flag))) {
                    return true;
                }
            }
            if (plot.getPermsOutsiders() != null) {
                return new String(plot.getPermsOutsiders()).contains(String.valueOf(flag));
            }
            return false;
        }
        if (city != null) {
            if (city.getCityMember(uuid.toString()) != null) {
                if (city.getCityMember(uuid.toString()).getCityRole().hasPermission(Role.ASSISTANT)) {
                    return true;
                }
            }
            CityPerms perms = city.getPlayerCityPerm(uuid);
            if (perms != null) {
                if (new String(perms.getPerms()).contains(String.valueOf(flag))) {
                    return true;
                }
            }
            if (city.getCityMember(uuid.toString()) != null && city.getMemberPerms() != null) {
                if (new String(city.getMemberPerms()).contains(String.valueOf(flag))) {
                    return true;
                }
            }
            if (city.getOutsiderPerms() != null) {
                return new String(city.getOutsiderPerms()).contains(String.valueOf(flag));
            }
        }
        return false;
    }

    public static boolean hasLocationPermissionFlags(UUID uuid, Location location, char flag) {
        City city = CityDatabase.getCityByClaim(location);
        if (city == null) {
            return true;
        }
        Plot plot = PlotDatabase.getCityPlot(city, location.toBlockLocation());
        if (plot == null) {
            return hasPermissionFlags(uuid, city, null, flag);
        } else {
            return hasPermissionFlags(uuid, city, plot, flag);
        }
    }


    public static void startSignEdit(Player player, int row, SignSide side, Sign sign) {
        if (sign.isWaxed()) {
            Metropolis.sendMessage(player, "messages.sign.edit.waxed");
            return;
        }
        Component line = side.line(row - 1);
        AnvilGui gui = new AnvilGui("Skyltredigering: Rad " + row);

        gui.setCost((short) 0);

        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta paperMeta = paper.getItemMeta();

        if (paperMeta != null) {
            paperMeta.displayName(line);
            paper.setItemMeta(paperMeta);
        }

        GuiItem paperItem = new GuiItem(paper, event -> {
            event.setCancelled(true);
        });

        StaticPane pane = new StaticPane(0, 0, 1, 1);
        pane.addItem(paperItem, 0, 0);
        gui.getFirstItemComponent().addPane(pane);

        ItemStack confirmStack = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirmStack.getItemMeta();
        confirmMeta.displayName(Component.text("Bekräfta").color(NamedTextColor.GREEN));
        confirmStack.setItemMeta(confirmMeta);

        GuiItem confirmItem = new GuiItem(confirmStack, event -> {
            String result = gui.getRenameText();
            if (!result.isEmpty()) {
                side.line(row - 1, MiniMessage.miniMessage().deserialize(result));
                sign.update();
                Apied.signEdit.remove(player);
                Metropolis.sendMessage(player, "messages.sign.edit.success");
            }

            player.closeInventory();
            event.setCancelled(true);
        });

        StaticPane confirmPane = new StaticPane(0, 0, 1, 1);
        confirmPane.addItem(confirmItem, 0, 0);
        gui.getResultComponent().addPane(confirmPane);

        gui.setOnGlobalClick(event -> event.setCancelled(true));

        gui.show(player);
    }
}
