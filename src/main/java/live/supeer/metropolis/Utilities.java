package live.supeer.metropolis;

import fr.mrmicky.fastboard.FastBoard;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.plot.Plot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import java.awt.*;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {
    static Metropolis plugin;

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

    public static long getTimestamp() {
        return System.currentTimeMillis() / 1000L;
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

    public static long parseTime(String length) {
        long time = 0;
        Matcher matcher = Pattern.compile("(\\d+)([smhdwy])").matcher(length);
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            switch (matcher.group(2)) {
                case "s":
                    time += value * 1000L;
                    break;
                case "m":
                    time += value * 60 * 1000L;
                    break;
                case "h":
                    time += value * 60 * 60 * 1000L;
                    break;
                case "d":
                    time += value * 24 * 60 * 60 * 1000L;
                    break;
                case "w":
                    time += value * 7 * 24 * 60 * 60 * 1000L;
                    break;
                case "y":
                    time += value * 365 * 24 * 60 * 60 * 1000L;
                    break;
            }
        }
        return time;
    }

    private static final Pattern timePattern = Pattern.compile("(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*(?:s[a-z]*)?)?", Pattern.CASE_INSENSITIVE);
    private static final int maxYears = 100000;

    public static String removeTimePattern(final String input) {
        return timePattern.matcher(input).replaceFirst("").trim();
    }

    public static long parseDateDiff(String time, boolean future) {
        return parseDateDiff(time, future, false);
    }

    public static long parseDateDiff(String time, boolean future, boolean emptyEpoch) {
        final Matcher m = timePattern.matcher(time);
        int years = 0;
        int months = 0;
        int weeks = 0;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        boolean found = false;
        while (m.find()) {
            if (m.group() == null || m.group().isEmpty()) {
                continue;
            }
            for (int i = 0; i < m.groupCount(); i++) {
                if (m.group(i) != null && !m.group(i).isEmpty()) {
                    found = true;
                    break;
                }
            }
            if (found) {
                if (m.group(1) != null && !m.group(1).isEmpty()) {
                    years = Integer.parseInt(m.group(1));
                }
                if (m.group(2) != null && !m.group(2).isEmpty()) {
                    months = Integer.parseInt(m.group(2));
                }
                if (m.group(3) != null && !m.group(3).isEmpty()) {
                    weeks = Integer.parseInt(m.group(3));
                }
                if (m.group(4) != null && !m.group(4).isEmpty()) {
                    days = Integer.parseInt(m.group(4));
                }
                if (m.group(5) != null && !m.group(5).isEmpty()) {
                    hours = Integer.parseInt(m.group(5));
                }
                if (m.group(6) != null && !m.group(6).isEmpty()) {
                    minutes = Integer.parseInt(m.group(6));
                }
                if (m.group(7) != null && !m.group(7).isEmpty()) {
                    seconds = Integer.parseInt(m.group(7));
                }
                break;
            }
        }
        if (!found) {
            return -1;
        }
        final Calendar c = new GregorianCalendar();

        if (emptyEpoch) {
            c.setTimeInMillis(0);
        }

        if (years > 0) {
            if (years > maxYears) {
                years = maxYears;
            }
            c.add(Calendar.YEAR, years * (future ? 1 : -1));
        }
        if (months > 0) {
            c.add(Calendar.MONTH, months * (future ? 1 : -1));
        }
        if (weeks > 0) {
            c.add(Calendar.WEEK_OF_YEAR, weeks * (future ? 1 : -1));
        }
        if (days > 0) {
            c.add(Calendar.DAY_OF_MONTH, days * (future ? 1 : -1));
        }
        if (hours > 0) {
            c.add(Calendar.HOUR_OF_DAY, hours * (future ? 1 : -1));
        }
        if (minutes > 0) {
            c.add(Calendar.MINUTE, minutes * (future ? 1 : -1));
        }
        if (seconds > 0) {
            c.add(Calendar.SECOND, seconds * (future ? 1 : -1));
        }
        final Calendar max = new GregorianCalendar();
        max.add(Calendar.YEAR, 10);
        if (c.after(max)) {
            return max.getTimeInMillis();
        }
        return c.getTimeInMillis();
    }

    static int dateDiff(final int type, final Calendar fromDate, final Calendar toDate, final boolean future) {
        final int year = Calendar.YEAR;

        final int fromYear = fromDate.get(year);
        final int toYear = toDate.get(year);
        if (Math.abs(fromYear - toYear) > maxYears) {
            toDate.set(year, fromYear +
                    (future ? maxYears : -maxYears));
        }

        int diff = 0;
        long savedDate = fromDate.getTimeInMillis();
        while ((future && !fromDate.after(toDate)) || (!future && !fromDate.before(toDate))) {
            savedDate = fromDate.getTimeInMillis();
            fromDate.add(type, future ? 1 : -1);
            diff++;
        }
        diff--;
        fromDate.setTimeInMillis(savedDate);
        return diff;
    }

    public static String formatDateDiff(final long date) {
        final Calendar c = new GregorianCalendar();
        c.setTimeInMillis(date);
        final Calendar now = new GregorianCalendar();
        return formatDateDiff(now, c);
    }

    public static String formatDateDiff(final Calendar fromDate, final Calendar toDate) {
        boolean future = false;
        if (toDate.equals(fromDate)) {
            return plugin.getMessage("messages.time.now");
        }
        if (toDate.after(fromDate)) {
            future = true;
        }
        // Temporary 50ms time buffer added to avoid display truncation due to code execution delays
        toDate.add(Calendar.MILLISECOND, future ? 50 : -50);
        final StringBuilder sb = new StringBuilder();
        final int[] types = new int[] {Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND};
        final String[] names = new String[] {plugin.getMessage("messages.time.year"), plugin.getMessage("messages.time.years"), plugin.getMessage("messages.time.month"), plugin.getMessage("messages.time.months"), plugin.getMessage("messages.time.day"), plugin.getMessage("messages.time.days"), plugin.getMessage("messages.time.hour"), plugin.getMessage("messages.time.hours"), plugin.getMessage("messages.time.minute"), plugin.getMessage("messages.time.minutes"), plugin.getMessage("messages.time.second"), plugin.getMessage("messages.time.seconds")};
        int accuracy = 0;
        for (int i = 0; i < types.length; i++) {
            if (accuracy > 2) {
                break;
            }
            final int diff = dateDiff(types[i], fromDate, toDate, future);
            if (diff > 0) {
                accuracy++;
                sb.append(" ").append(diff).append(" ").append(names[i * 2 + (diff > 1 ? 1 : 0)]);
            }
        }
        // Preserve correctness in the original date object by removing the extra buffer time
        toDate.add(Calendar.MILLISECOND, future ? -50 : 50);
        if (sb.length() == 0) {
            return plugin.getMessage("messages.time.now");
        }
        return sb.toString().trim();
    }

    public static Duration parseTimeToDuration(String length) {
        Duration duration = Duration.ZERO;
        Matcher matcher = Pattern.compile("(\\d+)([smhdwy])").matcher(length);
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            switch (matcher.group(2)) {
                case "s":
                    duration = duration.plusSeconds(value);
                    break;
                case "m":
                    duration = duration.plusMinutes(value);
                    break;
                case "h":
                    duration = duration.plusHours(value);
                    break;
                case "d":
                    duration = duration.plusDays(value);
                    break;
                case "w":
                    duration = duration.plusDays(value * 7L);
                    break;
                case "y":
                    duration = duration.plusDays(value * 365L);
                    break;
            }
        }
        return duration;
    }

    public static String parseTimeToReadable(String length) {
        Duration duration = parseTimeToDuration(length);
        long seconds = duration.getSeconds();

        long years = seconds / (365 * 24 * 3600);
        seconds %= 365 * 24 * 3600;

        long months = seconds / (30 * 24 * 3600);
        seconds %= 30 * 24 * 3600;

        long days = seconds / (24 * 3600);
        seconds %= 24 * 3600;

        long hours = seconds / 3600;
        seconds %= 3600;

        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder readableTime = new StringBuilder();
        if (years > 0) {
            readableTime.append(years).append(" ").append(plugin.getMessage("messages.time.years")).append(" ");
        }
        if (months > 0) {
            readableTime.append(months).append(" ").append(plugin.getMessage("messages.time.months")).append(" ");
        }
        if (days > 0) {
            readableTime.append(days).append(" ").append(plugin.getMessage("messages.time.days")).append(" ");
        }
        if (hours > 0) {
            readableTime.append(hours).append(" ").append(plugin.getMessage("messages.time.hours")).append(" ");
        }
        if (minutes > 0) {
            readableTime.append(minutes).append(" ").append(plugin.getMessage("messages.time.minutes")).append(" ");
        }
        if (seconds > 0) {
            readableTime.append(seconds).append(" ").append(plugin.getMessage("messages.time.seconds"));
        }

        return readableTime.toString().trim();
    }

    public static String formatTimeFromSeconds(int totalSeconds) {
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = (totalSeconds / (60 * 60)) % 24;
        int days = (totalSeconds / (24 * 60 * 60)) % 7;
        int weeks = (totalSeconds / (7 * 24 * 60 * 60)) % 52;
        int years = totalSeconds / (365 * 24 * 60 * 60);

        StringBuilder readableTime = new StringBuilder();
        if (years > 0) {
            readableTime.append(years).append(" ").append(plugin.getMessage("messages.time.years")).append(" ");
        }
        if (weeks > 0) {
            readableTime.append(weeks).append(" ").append(plugin.getMessage("messages.time.weeks")).append(" ");
        }
        if (days > 0) {
            readableTime.append(days).append(" ").append(plugin.getMessage("messages.time.days")).append(" ");
        }
        if (hours > 0) {
            readableTime.append(hours).append(" ").append(plugin.getMessage("messages.time.hours")).append(" ");
        }
        if (minutes > 0) {
            readableTime.append(minutes).append(" ").append(plugin.getMessage("messages.time.minutes")).append(" ");
        }
        if (seconds > 0) {
            readableTime.append(seconds).append(" ").append(plugin.getMessage("messages.time.seconds"));
        }

        return readableTime.toString().trim();
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

    public static String niceDate(long timestamp) {
        if (timestamp == 0) {
            return "unknown";
        }

        long rightNow = getTimestamp();

        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(timestamp * 1000);

        Calendar dateNow = Calendar.getInstance();
        dateNow.setTimeInMillis(rightNow * 1000);

        String day;
        String[] months = {
                plugin.getMessage("messages.months.january"),
                plugin.getMessage("messages.months.february"),
                plugin.getMessage("messages.months.march"),
                plugin.getMessage("messages.months.april"),
                plugin.getMessage("messages.months.may"),
                plugin.getMessage("messages.months.june"),
                plugin.getMessage("messages.months.july"),
                plugin.getMessage("messages.months.august"),
                plugin.getMessage("messages.months.september"),
                plugin.getMessage("messages.months.october"),
                plugin.getMessage("messages.months.november"),
                plugin.getMessage("messages.months.december")
        };

        if (date.get(Calendar.DAY_OF_MONTH) == dateNow.get(Calendar.DAY_OF_MONTH)
                && date.get(Calendar.MONTH) == dateNow.get(Calendar.MONTH)
                && date.get(Calendar.YEAR) == dateNow.get(Calendar.YEAR)) {
            day = plugin.getMessage("messages.days.today");
        } else {
            Calendar dateYesterday = Calendar.getInstance();
            dateYesterday.setTimeInMillis((rightNow - 86400) * 1000);

            day =
                    date.get(Calendar.DAY_OF_MONTH) == dateYesterday.get(Calendar.DAY_OF_MONTH)
                            && date.get(Calendar.MONTH) == dateYesterday.get(Calendar.MONTH)
                            && date.get(Calendar.YEAR) == dateYesterday.get(Calendar.YEAR)
                            ? plugin.getMessage("messages.days.yesterday")
                            : date.get(Calendar.DAY_OF_MONTH)
                            + " "
                            + months[date.get(Calendar.MONTH)]
                            + (dateNow.get(Calendar.YEAR) != date.get(Calendar.YEAR)
                            ? " " + date.get(Calendar.YEAR)
                            : "");
        }

        return day
                + ", "
                + String.format("%02d", date.get(Calendar.HOUR_OF_DAY))
                + ":"
                + String.format("%02d", date.get(Calendar.MINUTE));
    }

    public static ItemStack letterBanner(String letter, String lore) {
        String letterLower = letter.toLowerCase();
        ItemStack banner = new ItemStack(org.bukkit.Material.WHITE_BANNER);
        BannerMeta bannerMeta = (BannerMeta) banner.getItemMeta();
        bannerMeta.displayName(Component.text(lore).color(NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, true));

        bannerMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        if (Objects.equals(letterLower, "a")
                || Objects.equals(letterLower, "å")
                || Objects.equals(letterLower, "ä")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "b")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.CURLY_BORDER));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "c")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "d")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.CURLY_BORDER));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "e")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "f")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "g")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "h")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.HALF_HORIZONTAL));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.HALF_HORIZONTAL_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "i")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_CENTER));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "j")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "k")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNRIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNLEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "l")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "m")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.TRIANGLE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.TRIANGLES_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "n")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.TRIANGLE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNRIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "o") || Objects.equals(letterLower, "ö")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "p")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "q")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.HALF_HORIZONTAL));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.HALF_HORIZONTAL_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.RHOMBUS));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.SQUARE_BOTTOM_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "r")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.HALF_HORIZONTAL));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNRIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "s")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.RHOMBUS));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNRIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.CURLY_BORDER));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "t")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_CENTER));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "u")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "v")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNLEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.TRIANGLE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNLEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "w")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.TRIANGLE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.TRIANGLES_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "x")) {
            bannerMeta.addPattern(new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.CROSS));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "y")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNRIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNLEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "z")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNLEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "0")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNLEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));

        }
        if (Objects.equals(letterLower, "1")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_CENTER));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.SQUARE_TOP_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.CURLY_BORDER));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "2")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.RHOMBUS));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNLEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "3")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.CURLY_BORDER));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "4")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "5")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.RHOMBUS));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNRIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "6")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "7")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNLEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "8")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        if (Objects.equals(letterLower, "9")) {
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
            bannerMeta.addPattern(
                    new org.bukkit.block.banner.Pattern(DyeColor.WHITE, PatternType.BORDER));
        }
        banner.setItemMeta(bannerMeta);
        return banner;
    }
}
