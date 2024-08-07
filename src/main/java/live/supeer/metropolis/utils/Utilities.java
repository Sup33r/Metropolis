package live.supeer.metropolis.utils;

import fr.mrmicky.fastboard.FastBoard;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import live.supeer.metropolis.city.Role;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.plot.PlotDatabase;
import org.bukkit.*;
import org.bukkit.entity.Player;

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
                            Objects.requireNonNull(CityDatabase.getClaim(chunkLocation)).getCityName(),
                            HCDatabase.getHomeCityToCityname(player.getUniqueId().toString()))) {
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

    public static void sendCityScoreboard(Player player, City city, Plot plot) {
        FastBoard board = new FastBoard(player);
        int i = 0;
        board.updateTitle("§a             §l" + city.getCityName() + "§r             ");
        board.updateLine(i, " ");
        i = i + 1;
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
        sendCityScoreboard(player, city, plot);
    }
    public static City hasCityPermissions(Player player, String permission, Role targetRole) {
        if (!player.hasPermission(permission)) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return null;
        }
        City city = HCDatabase.getHomeCityToCity(player.getUniqueId().toString());
        if (city == null) {
            plugin.sendMessage(player, "messages.error.missing.homecity");
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
}
