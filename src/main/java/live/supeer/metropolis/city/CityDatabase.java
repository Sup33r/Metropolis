package live.supeer.metropolis.city;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import live.supeer.metropolis.Database;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.utils.LocationUtil;
import live.supeer.metropolis.utils.Utilities;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.utils.DateUtil;
import org.bukkit.*;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.intellij.lang.annotations.Language;
import org.locationtech.jts.geom.*;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class CityDatabase {
    public static Metropolis plugin;
    private static final List<City> cities = new ArrayList<>();

    public static void initDBSync() throws SQLException {
        loadCities();
    }

    private static void loadCities() throws SQLException {
        var dbRows = DB.getResults("SELECT * FROM `mp_cities`;");

        for (DbRow dbRow : dbRows) {
            City city = new City(dbRow);
            cities.add(city);
            plugin.getLogger().info("Loaded city " + city.getCityName());
            loadMembers(city);
            loadClaims(city);
            loadPlots(city);
        }
    }

    private static void loadMembers(City rCity) throws SQLException {

        var members = DB.getResults("SELECT * FROM `mp_members` WHERE `cityId` = '" + rCity.getCityId() + "';");
        for (DbRow member : members) {
            rCity.addCityMember(new Member(member));
        }
    }

//    private static void loadCityBans(City rCity) throws SQLException {
//        var bans = DB.getResults("SELECT * FROM `mp_citybans` WHERE `cityId` = '" + rCity.getCityId() + "';");
//        for (DbRow ban : bans) {
//            rCity.addCityBan(new Ban(ban));
//        }
//    }

    private static void loadClaims(City rCity) throws SQLException {
        String cityName = rCity.getCityName();
        Bukkit.broadcastMessage(cityName);
        var claims = DB.getResults("SELECT * FROM `mp_claims` WHERE `cityName` = '" + cityName + "';");
        for (DbRow claim : claims) {
            Claim claim1 = new Claim(claim);
            rCity.addCityClaim(claim1);
            plugin.getLogger().info("Loaded claim " + claim1.getXPosition() + " | " + claim1.getZPosition() + "  |  " + claim1.getClaimWorld() + " for city " + cityName);
        }
    }

    private static void loadPlots(City rCity) throws SQLException {
        String cityName = rCity.getCityName();
        Bukkit.broadcastMessage(cityName);
        var plots = DB.getResults("SELECT * FROM `mp_plots` WHERE `cityName` = '" + cityName + "';");
        for (DbRow plot : plots) {
            Plot plot1 = new Plot(plot);
            rCity.addCityPlot(plot1);
        }
    }


    public static City newCity(String cityName, Player player) {
        try {
            DB.executeUpdate("INSERT INTO `mp_cities` (`cityName`, `originalMayorUUID`, `originalMayorName`, `cityBalance`, `citySpawn`, `createDate`) VALUES (" +
                    Database.sqlString(cityName) + ", " +
                    Database.sqlString(player.getUniqueId().toString()) + ", " +
                    Database.sqlString(player.getName()) + ", " +
                    Metropolis.configuration.getCityStartingBalance() + ", " +
                    Database.sqlString(LocationUtil.locationToString(player.getLocation())) + ", " +
                    DateUtil.getTimestamp() + ");");

            City city = new City(DB.getFirstRow("SELECT * FROM `mp_cities` WHERE `cityName` = " + Database.sqlString(cityName) + ";"));
            cities.add(city);
            newMember(city, player);
            plugin.getLogger().info(player.getName() + " created a new city: " + cityName);
            return city;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void newMember(City city, Player player) {
        try {
            String cityName = city.getCityName();
            DB.executeUpdate("INSERT INTO `mp_members` (`playerName`, `playerUUID`, `cityId`, `cityName`, `cityRole`, `joinDate`) VALUES (" + Database.sqlString(player.getName()) + ", " + Database.sqlString(player.getUniqueId().toString()) + ", " + city.getCityId() + ", " + Database.sqlString(cityName) + ", " + Database.sqlString(Role.MEMBER.getRoleName()) + ", " + DateUtil.getTimestamp() + ");");
            city.addCityMember(
                    new Member(
                            DB.getFirstRow(
                                    "SELECT * FROM `mp_members` WHERE `cityName` = "
                                            + Database.sqlString(cityName)
                                            + " AND `playerUUID` = "
                                            + Database.sqlString(player.getUniqueId().toString())
                                            + ";")));
            HCDatabase.setHomeCity(player.getUniqueId().toString(), city);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Claim createClaim(City city, Location location, boolean outpost, String playername, String playerUUID) {
        try {
            String cityName = city.getCityName();
            DB.executeInsert("INSERT INTO `mp_claims` (`claimerName`, `claimerUUID`, `world`, `xPosition`, `zPosition`, `claimDate`, `cityName`, `outpost`) VALUES (" + Database.sqlString(playername) + ", " + Database.sqlString(playerUUID) + ", '" + location.getChunk().getWorld().getName() + "', " + location.getChunk().getX() + ", " + location.getChunk().getZ() + ", " + DateUtil.getTimestamp() + ", '" + cityName + "', " + outpost + ");");
            city.addCityClaim(new Claim(DB.getFirstRow("SELECT * FROM `mp_claims` WHERE `cityName` = " + Database.sqlString(cityName) + " AND `xPosition` = " + location.getChunk().getX() + " AND `zPosition` = " + location.getChunk().getZ() + ";")));
            return city.getCityClaim(location);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static District createDistrict(City city, Polygon districtPoints, String districtName, World world) {
        try {
            DB.executeInsert("INSERT INTO `mp_districts` (`districtName`, `cityId`, `world`, `districtPoints`, `districtBoundary`, `contactPlayers`) VALUES (" + Database.sqlString(districtName) + ", " + city.getCityId() + ", " + Database.sqlString(world.getName()) + ", " + Database.sqlString(LocationUtil.polygonToString(districtPoints)) + ", " + "ST_GeomFromText('" + districtPoints.toText() + "'), " + Database.sqlString(null) + ");");
            return new District(DB.getFirstRow("SELECT * FROM `mp_districts` WHERE `cityId` = " + city.getCityId() + " AND `districtName` = " + Database.sqlString(districtName) + ";"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void newCityGo(Location location, String name, City city) {
        try {
            DB.executeInsert("INSERT INTO `mp_citygoes` (`cityId`, `goName`, `goLocation`, `createDate`) VALUES (" + city.getCityId() + ", " + Database.sqlString(name) + ", " + Database.sqlString(LocationUtil.locationToString(location)) + ", " + DateUtil.getTimestamp() + ");");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean cityGoExists(String name, City city) {
        try {
            var results = DB.getResults("SELECT * FROM `mp_citygoes` WHERE `cityId` = " + city.getCityId() + " AND `goName` = " + Database.sqlString(name) + ";");
            return !results.isEmpty();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean memberExists(String playerUUID, City city) {
        try {
            var results = DB.getResults("SELECT * FROM `mp_members` WHERE `cityId` = " + city.getCityId() + " AND `playerUUID` = " + Database.sqlString(playerUUID) + ";");
            return !results.isEmpty();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }




    public static int getCityGoCount(City city, Role role) {
        if (role.equals(Role.MEMBER)) {
            try {
                var results = DB.getResults("SELECT * FROM `mp_citygoes` WHERE `cityId` = " + city.getCityId() + " AND `accessLevel` IS NULL ;");
                return results.size();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (role.equals(Role.INVITER)) {
            try {
                var results = DB.getResults("SELECT * FROM `mp_citygoes` WHERE `cityId` = " + city.getCityId() + " AND `accessLevel` IS NULL OR `accessLevel` = 'inviter';");
                return results.size();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (role.equals(Role.ASSISTANT)) {
            try {
                var results = DB.getResults("SELECT * FROM `mp_citygoes` WHERE `cityId` = " + city.getCityId() + " AND `accessLevel` IS NULL OR `accessLevel` = 'inviter' OR `accessLevel` = 'assistant';");
                return results.size();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (role.equals(Role.VICE_MAYOR)) {
            try {
                var results = DB.getResults("SELECT * FROM `mp_citygoes` WHERE `cityId` = " + city.getCityId() + " AND `accessLevel` IS NULL OR `accessLevel` = 'inviter' OR `accessLevel` = 'assistant' OR `accessLevel` = 'vicemayor';");
                return results.size();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (role.equals(Role.MAYOR)) {
            try {
                var results = DB.getResults("SELECT * FROM `mp_citygoes` WHERE `cityId` = " + city.getCityId() + ";");
                return results.size();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public static List<City> getCityList(Player player, int count, String searchterm) {
        List<City> cityList = new ArrayList<>();
        try {
            String query;
            if (searchterm == null) {
                query = "SELECT * FROM `mp_cities` ORDER BY `cityBalance` DESC LIMIT " + count + ";";
            } else {
                query = "SELECT * FROM `mp_cities` AND `cityName` LIKE " + Database.sqlString("%" + searchterm + "%") + " ORDER BY `cityBalance` DESC LIMIT " + count + ";";
            }
            var results = DB.getResults(query);
            for (var row : results) {
                City city = new City(row);
                if (city.getCityMember(player.getUniqueId().toString()) != null || city.isPublic()) {
                    cityList.add(city);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cityList;
    }

    public static List<String> getCityGoNames(City city, Role role) {
        if (role.equals(Role.MEMBER)) {
            try {
                var results = DB.getResults("SELECT * FROM `mp_citygoes` WHERE `cityId` = " + city.getCityId() + " AND `accessLevel` IS NULL ;");
                return results.stream().map(result -> result.getString("goName")).collect(Collectors.toList());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (role.equals(Role.INVITER)) {
            try {
                var results = DB.getResults("SELECT * FROM `mp_citygoes` WHERE `cityId` = " + city.getCityId() + " AND `accessLevel` IS NULL OR `accessLevel` = 'inviter';");
                return results.stream().map(result -> result.getString("goName")).collect(Collectors.toList());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (role.equals(Role.ASSISTANT)) {
            try {
                var results = DB.getResults("SELECT * FROM `mp_citygoes` WHERE `cityId` = " + city.getCityId() + " AND `accessLevel` IS NULL OR `accessLevel` = 'inviter' OR `accessLevel` = 'assistant';");
                return results.stream().map(result -> result.getString("goName")).collect(Collectors.toList());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (role.equals(Role.VICE_MAYOR)) {
            try {
                var results = DB.getResults("SELECT * FROM `mp_citygoes` WHERE `cityId` = " + city.getCityId() + " AND `accessLevel` IS NULL OR `accessLevel` = 'inviter' OR `accessLevel` = 'assistant' OR `accessLevel` = 'vicemayor';");
                return results.stream().map(result -> result.getString("goName")).collect(Collectors.toList());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (role.equals(Role.MAYOR)) {
            try {
                var results = DB.getResults("SELECT * FROM `mp_citygoes` WHERE `cityId` = " + city.getCityId() + ";");
                return results.stream().map(result -> result.getString("goName")).collect(Collectors.toList());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static Location getCityGoLocation(String name, City city) {
        try {
            var results = DB.getResults("SELECT * FROM `mp_citygoes` WHERE `cityId` = " + city.getCityId() + " AND `goName` = " + Database.sqlString(name) + ";");
            if (!results.isEmpty()) {
                return LocationUtil.stringToLocation(results.get(0).getString("goLocation"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getCityGoAccessLevel(String name, City city) {
        try {
            var results = DB.getResults("SELECT * FROM `mp_citygoes` WHERE `cityId` = " + city.getCityId() + " AND `goName` = " + Database.sqlString(name) + ";");
            if (!results.isEmpty()) {
                return results.get(0).getString("accessLevel");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setCityGoAccessLevel(String name, City city, String accessLevel) {
        try {
            DB.executeUpdate("UPDATE `mp_citygoes` SET `accessLevel` = " + Database.sqlString(accessLevel) + " WHERE `cityId` = " + city.getCityId() + " AND `goName` = " + Database.sqlString(name) + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void setCityGoDisplayname(String name, City city, String displayname) {
        try {
            DB.executeUpdate("UPDATE `mp_citygoes` SET `goNickname` = " + Database.sqlString(displayname) + " WHERE `cityId` = " + city.getCityId() + " AND `goName` = " + Database.sqlString(name) + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void setCityGoName(String name, City city, String newName) {
        try {
            DB.executeUpdate("UPDATE `mp_citygoes` SET `goName` = '" + Database.sqlString(newName) + "' WHERE `cityId` = " + city.getCityId() + " AND `goName` = " + Database.sqlString(name) + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getCityGoDisplayname(String name, City city) {
        try {
            var results = DB.getResults("SELECT * FROM `mp_citygoes` WHERE `cityId` = " + city.getCityId() + " AND `goName` = " + Database.sqlString(name) + ";");
            if (results.size() == 1) {
                return results.get(0).getString("goNickname");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void deleteGo(String name, City city) {
        try {
            DB.executeUpdate("DELETE FROM `mp_citygoes` WHERE `cityId` = " + city.getCityId() + " AND `goName` = " + Database.sqlString(name) + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteClaim(Claim claim) {
        try {
            DB.executeUpdate("DELETE FROM `mp_claims` WHERE `claimId` = " + claim.getClaimId() + ";");
            claim.getCity().removeCityClaim(claim);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Optional<City> getCity(String cityName) {
        for (City city : cities) {
            if (city.getCityName().equalsIgnoreCase(cityName)) return Optional.of(city);
        }
        return Optional.empty();
    }

    public static Optional<City> getCity(int id) {
        for (City city : cities) {
            if (city.getCityId() == id) return Optional.of(city);
        }
        return Optional.empty();
    }

    public static Claim getClaim(Location location) {
        for (City city : cities) {
            if (city.getCityClaim(location) != null) {
                return city.getCityClaim(location);
            }
        }
        return null;
    }

    public static City getCityByClaim(Location location) {
        for (City city : cities) {
            if (city.getCityClaim(location) != null) {
                return city;
            }
        }
        return null;
    }


    public static Role getCityRole(City city, String playerUUID) {
        try {
            if (DB.getResults("SELECT * FROM `mp_members` WHERE `playerUUID` = " + Database.sqlString(playerUUID) + " AND `cityName` = " + Database.sqlString(city.getCityName()) + ";").isEmpty())
                return null;
            return Role.fromString(DB.getFirstRow("SELECT * FROM `mp_members` WHERE `playerUUID` = " + Database.sqlString(playerUUID) + " AND `cityName` = " + Database.sqlString(city.getCityName()) + ";").getString("cityRole"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setCityRole(City city, String playerUUID, Role role) {
        try {
            DB.executeUpdate("UPDATE `mp_members` SET `cityRole` = " + Database.sqlString(role.getRoleName()) + " WHERE `playerUUID` = " + Database.sqlString(playerUUID) + " AND `cityName` = " + Database.sqlString(city.getCityName()) + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int getPlayerCityCount(String playerUUID) {
        try {
            return DB.getResults("SELECT * FROM `mp_members` WHERE `playerUUID` = " + Database.sqlString(playerUUID) + ";").size();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getCityBalance(City city) {
        return city.getCityBalance();
    }

    public static void addCityBalance(City city, int amount) {
        city.addCityBalance(amount);
    }

    public static void removeCityBalance(City city, int amount) {
        city.removeCityBalance(amount);
    }

    public static void deleteCity(City city) {
        try {
            removeCityMembers(city);
            removeCityPlots(city);
            removeCityHCs(city);
            removeCityPlotPerms(city);
            removeCityPerms(city);
            removeCityBans(city);
            removeCityGoes(city);
            removeCityClaims(city);
            removeCityDistricts(city);
            DB.executeUpdate("DELETE FROM `mp_cities` WHERE `cityId` = " + city.getCityId() + ";");
            cities.remove(city);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeCityMembers(City city) {
        try {
            DB.executeUpdate("DELETE FROM `mp_members` WHERE `cityId` = " + city.getCityId() + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeCityPlots(City city) {
        try {
            DB.executeUpdate("DELETE FROM `mp_plots` WHERE `cityId` = " + city.getCityId() + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeCityHCs(City city) {
        try {
            DB.executeUpdate("DELETE FROM `mp_homecities` WHERE `cityId` = " + city.getCityId() + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeCityPlotPerms(City city) {
        try {
            DB.executeUpdate("DELETE FROM `mp_plotperms` WHERE `cityId` = " + city.getCityId() + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeCityPerms(City city) {
        try {
            DB.executeUpdate("DELETE FROM `mp_cityperms` WHERE `cityId` = " + city.getCityId() + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeCityBans(City city) {
        try {
            DB.executeUpdate("DELETE FROM `mp_citybans` WHERE `cityId` = " + city.getCityId() + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeCityGoes(City city) {
        try {
            DB.executeUpdate("DELETE FROM `mp_citygoes` WHERE `cityId` = " + city.getCityId() + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeCityClaims(City city) {
        try {
            DB.executeUpdate("DELETE FROM `mp_claims` WHERE `cityName` = " + Database.sqlString(city.getCityName()) + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeCityDistricts(City city) {
        try {
            DB.executeUpdate("DELETE FROM `mp_districts` WHERE `cityId` = " + city.getCityId() + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String[] memberCityList(String uuid) {
        try {
            return DB.getResults("SELECT `cityName` FROM `mp_members` WHERE `playerUUID` = " + Database.sqlString(uuid) + ";").stream().map(row -> row.getString("cityName")).toArray(String[]::new);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getLatestNameChange(City city) {
        try {
            return DB.getFirstRow("SELECT * FROM `mp_cities` WHERE `cityName` = " + Database.sqlString(city.getCityName()) + ";").getInt("latestNameChange");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void setLatestNameChange(City city, int timestamp) {
        try {
            DB.executeUpdate("UPDATE `mp_cities` SET `latestNameChange` = " + timestamp + " WHERE `cityName` = " + Database.sqlString(city.getCityName()) + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean hasClaim(int x, int z, World world) {
        try {
            return !DB.getResults("SELECT * FROM `mp_claims` WHERE `world` = '" + world + "' AND `xPosition` = " + x + " AND `zPosition` = " + z + ";").isEmpty();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void setCityMessage(City city, String messageType, String message) {
        if (message == null) {
            if (messageType.equals("enterMessage")) city.setEnterMessage(null);
            if (messageType.equals("exitMessage")) city.setExitMessage(null);
            if (messageType.equals("motdMessage")) city.setMotdMessage(null);
            return;
        }
        if (messageType.equals("enterMessage")) city.setEnterMessage(message);
        if (messageType.equals("exitMessage")) city.setExitMessage(message);
        if (messageType.equals("motdMessage")) city.setMotdMessage(message);
    }

    public static String getCityMembers(City city) {
        try {
            var results = DB.getResults("SELECT * FROM `mp_members` WHERE `cityName` = " + Database.sqlString(city.getCityName()) + ";");
            if (results.isEmpty()) return null;
            StringBuilder sb = new StringBuilder();
            for (var row : results) {
                sb.append("§2").append(row.getString("playerName")).append("§a, ");
            }
            sb.delete(sb.length() - 2, sb.length());
            return sb.toString();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getCityMemberCount(City city) {
        try {
            return DB.getResults("SELECT * FROM `mp_members` WHERE `cityName` = " + Database.sqlString(city.getCityName()) + ";").size();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void addCityBan(City city, String playerUUID, String reason, Player placer, long placeDate, long length) {
        try {
            DB.executeUpdate("INSERT INTO `mp_citybans` (`cityId`, `playerUUID`, `placeDate`, `length`, `reason`, `placeUUID`) VALUES (" + city.getCityId() + ", " + Database.sqlString(playerUUID) + ", " + placeDate + ", " + length + ", " + Database.sqlString(reason) + ", " + Database.sqlString(placer.getUniqueId().toString()) + ");");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static List<Ban> getCityBans(City city) {
        removeExpiredBans(city);
        try {
            var results = DB.getResults("SELECT * FROM `mp_citybans` WHERE `cityId` = " + city.getCityId() + ";");
            if (results.isEmpty()) return null;
            List<Ban> bans = new ArrayList<>();
            for (var row : results) {
                bans.add(new Ban(row));
            }
            return bans;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Ban getCityBan(City city, String playerUUID) {
        removeExpiredBans(city);
        try {
            var results = DB.getResults("SELECT * FROM `mp_citybans` WHERE `cityId` = " + city.getCityId() + " AND `playerUUID` = " + Database.sqlString(playerUUID) + ";");
            if (results.isEmpty()) return null;
            var row = results.get(0);
            return new Ban(row);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void removeCityBan(City city, Ban ban) {
        try {
            DB.executeUpdate("DELETE FROM `mp_citybans` WHERE `cityId` = " + city.getCityId() + " AND `playerUUID` = " + Database.sqlString(ban.getPlayerUUID()) + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void removeExpiredBans(City city) {
        try {
            long currentTime = System.currentTimeMillis();
            DB.executeUpdate("DELETE FROM `mp_citybans` WHERE `cityId` = " + city.getCityId() + " AND `length` < " + currentTime + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<CityDistance> getCitiesWithinRadius(Location center, int radius) {
        List<CityDistance> result = new ArrayList<>();
        int chunkRadius = (radius / 16) + 1; // Convert block radius to chunk radius

        try {
            // Query claims within a square area
            String sql = "SELECT DISTINCT c.cityName, cl.xPosition, cl.zPosition " +
                    "FROM mp_claims cl " +
                    "JOIN mp_cities c ON cl.cityName = c.cityName " +
                    "WHERE cl.world = ? " +
                    "AND cl.xPosition BETWEEN ? AND ? " +
                    "AND cl.zPosition BETWEEN ? AND ?";

            List<DbRow> rows = DB.getResults(sql,
                    center.getWorld().getName(),
                    center.getChunk().getX() - chunkRadius,
                    center.getChunk().getX() + chunkRadius,
                    center.getChunk().getZ() - chunkRadius,
                    center.getChunk().getZ() + chunkRadius
            );

            Map<String, Location> cityLocations = new HashMap<>();

            for (DbRow row : rows) {
                String cityName = row.getString("cityName");
                int chunkX = row.getInt("xPosition");
                int chunkZ = row.getInt("zPosition");

                // Convert chunk coordinates to block coordinates (center of the chunk)
                Location claimLocation = new Location(center.getWorld(),
                        chunkX * 16 + 8,
                        center.getY(),
                        chunkZ * 16 + 8);

                // Keep only the nearest claim for each city
                if (!cityLocations.containsKey(cityName) ||
                        claimLocation.distanceSquared(center) < cityLocations.get(cityName).distanceSquared(center)) {
                    cityLocations.put(cityName, claimLocation);
                }
            }

            // Calculate precise distances and filter by actual radius
            for (Map.Entry<String, Location> entry : cityLocations.entrySet()) {
                String cityName = entry.getKey();
                Location claimLocation = entry.getValue();
                double distance = Math.sqrt(claimLocation.distanceSquared(center));

                if (distance <= radius) {
                    Optional<City> cityOpt = getCity(cityName);
                    if (cityOpt.isPresent()) {
                        result.add(new CityDistance(cityOpt.get(), (int) distance));
                    }
                }
            }

            // Sort results by distance
            result.sort(Comparator.comparingInt(CityDistance::getDistance));

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static Plot getPlotAtLocation(Location location) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Point point = geometryFactory.createPoint(new Coordinate(location.getX(), location.getZ()));
        int y = location.getBlockY();
        String worldName = location.getWorld().getName();

        try {
            DbRow result = DB.getFirstRow(
                    "SELECT * FROM `mp_plots` WHERE ST_Intersects(`plotBoundary`, ST_GeomFromText(?)) AND `plotYMin` <= ? AND `plotYMax` >= ? AND `plotCenter` LIKE ?;",
                    point.toText()
                    , y
                    , y
                    , worldName + "%"
            );

            if (result != null) {
                return new Plot(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static District getDistrict(Location location) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Point point = geometryFactory.createPoint(new Coordinate(location.getX(), location.getZ()));
        String worldName = location.getWorld().getName();

        try {
            DbRow result = DB.getFirstRow(
                    "SELECT * FROM `mp_districts` WHERE ST_Contains(`districtBoundary`, ST_GeomFromText(?)) AND `world` = ?;",
                    point.toText()
                    , worldName
            );

            if (result != null) {
                return new District(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static District getDistrict(String districtName, City city) {
        try {
            DbRow result = DB.getFirstRow("SELECT * FROM `mp_districts` WHERE `districtName` = " + Database.sqlString(districtName) + " AND `cityId` = " + city.getCityId() + ";");
            if (result != null) {
                return new District(result);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

        public static void deleteDistrict(District district) {
        try {
            DB.executeUpdate("DELETE FROM `mp_districts` WHERE `districtName` = " + Database.sqlString(district.getDistrictName()) + " AND `cityId` = " + district.getCity().getCityId() + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<District> getDistricts(City city) {
        List<District> districts = new ArrayList<>();
        try {
            var results = DB.getResults("SELECT * FROM `mp_districts` WHERE `cityId` = " + city.getCityId() + ";");
            for (var row : results) {
                districts.add(new District(row));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return districts;
    }

    public static boolean districtExists(String districtName, City city) {
        try {
            return !DB.getResults("SELECT * FROM `mp_districts` WHERE `districtName` = " + Database.sqlString(districtName) + " AND `cityId` = " + city.getCityId() + ";").isEmpty();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isDistrictInClaim(Claim claim) {
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
                    "SELECT COUNT(*) as count FROM `mp_districts` WHERE ST_Contains(ST_GeomFromText(?), ST_Centroid(`districtBoundary`)) AND `world` = ?;",
                    polygonWKT,
                    world.getName()
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

    public static boolean hasCityGoInClaim(Claim claim) {
        World world = claim.getClaimWorld();
        int chunkX = claim.getXPosition();
        int chunkZ = claim.getZPosition();

        // Calculate the block coordinates of the chunk
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        try {
            String sql = "SELECT COUNT(*) as count FROM `mp_citygoes` WHERE " +
                    "SUBSTRING_INDEX(`goLocation`, ',', 1) >= ? AND " +
                    "SUBSTRING_INDEX(`goLocation`, ',', 1) <= ? AND " +
                    "SUBSTRING_INDEX(SUBSTRING_INDEX(`goLocation`, ',', 3), ',', -1) >= ? AND " +
                    "SUBSTRING_INDEX(SUBSTRING_INDEX(`goLocation`, ',', 3), ',', -1) <= ? AND " +
                    "SUBSTRING_INDEX(`goLocation`, ',', -1) = ?";

            List<DbRow> results = DB.getResults(sql, minX, maxX, minZ, maxZ, world.getName());

            if (!results.isEmpty()) {
                int count = results.get(0).getInt("count");
                return count > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean isCitySpawnInClaim(Claim claim) {
        City city = claim.getCity();
        if (city == null) {
            return false;
        }

        Location citySpawn = city.getCitySpawn();
        if (citySpawn == null || !citySpawn.getWorld().equals(claim.getClaimWorld())) {
            return false;
        }

        int spawnChunkX = citySpawn.getBlockX() >> 4;
        int spawnChunkZ = citySpawn.getBlockZ() >> 4;

        return spawnChunkX == claim.getXPosition() && spawnChunkZ == claim.getZPosition();
    }

}
