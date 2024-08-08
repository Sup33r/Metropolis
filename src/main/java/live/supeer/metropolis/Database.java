package live.supeer.metropolis;

import co.aikar.idb.BukkitDB;
import co.aikar.idb.DB;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import live.supeer.metropolis.utils.DateUtil;

import java.sql.SQLException;

public class Database {

    public static Metropolis plugin;

    public static void initialize() {
        try {
            BukkitDB.createHikariDatabase(
                    Metropolis.getPlugin(),
                    Metropolis.configuration.getSqlUsername(),
                    Metropolis.configuration.getSqlPassword(),
                    Metropolis.configuration.getSqlDatabase(),
                    Metropolis.configuration.getSqlHost()
                            + ":"
                            + Metropolis.configuration.getSqlPort());
            createTables();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize database, disabling plugin.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    public static void synchronize() {
        try {
            CityDatabase.initDBSync();
        } catch (Exception exception) {
            exception.printStackTrace();
            plugin.getLogger().warning("Failed to synchronize database, disabling plugin.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    public static String sqlString(String string) {
        return string == null ? "NULL" : "'" + string.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    public static void createTables() {
        try {

            DB.executeUpdate(
                    """
                              CREATE TABLE IF NOT EXISTS `mp_cities` (
                                `cityID` int(11) NOT NULL AUTO_INCREMENT,
                                `cityName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `originalMayorUUID` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `originalMayorName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `cityBalance` int(25) NOT NULL,
                                `cityTax` double DEFAULT 2,
                                `bonusClaims` int(11) DEFAULT 0,
                                `citySpawn` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `createDate` bigint(30) DEFAULT NULL,
                                `latestNameChange` bigint(30) DEFAULT NULL,
                                `enterMessage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                `exitMessage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                `motdMessage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                `isOpen` tinyint(1) DEFAULT 0,
                                `isPublic` tinyint(1) DEFAULT 0,
                                `isRemoved` tinyint(1) NOT NULL,
                                `cityBoundary` GEOMETRY NOT NULL,
                                PRIMARY KEY (`cityID`),
                                SPATIAL INDEX `idx_cityBoundary` (`cityBoundary`)
                              ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;""");

            DB.executeUpdate(
                    """
                              CREATE TABLE IF NOT EXISTS `mp_members` (
                                `playerName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `playerUUID` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `cityID` int(11) NOT NULL,
                                `cityName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `cityRole` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                `joinDate` bigint(30) DEFAULT NULL,
                                PRIMARY KEY (cityID,playerUUID)
                              ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""");

            DB.executeUpdate(
                    """
                              CREATE TABLE IF NOT EXISTS `mp_homecities` (
                                `playerUUID` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `playerName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `cityName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                PRIMARY KEY (`playerUUID`)
                              ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""");

            DB.executeUpdate(
                    """
                              CREATE TABLE IF NOT EXISTS `mp_claims` (
                                `claimId` int(11) NOT NULL AUTO_INCREMENT,
                                `claimerName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `claimerUUID` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `world` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `xPosition` mediumint(9) NOT NULL,
                                `zPosition` mediumint(9) NOT NULL,
                                `claimDate` bigint(30) DEFAULT NULL,
                                `cityName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `outpost` tinyint(1) DEFAULT '0',
                                PRIMARY KEY (`claimId`)
                              ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""");

            DB.executeUpdate(
                    """
                              CREATE TABLE IF NOT EXISTS `mp_citylogs` (
                                `logId` int(11) NOT NULL AUTO_INCREMENT,
                                `cityId` int(11) NOT NULL,
                                `dateTime` bigint(30) DEFAULT NULL,
                                `jsonLog` text NOT NULL,
                                PRIMARY KEY (logId)
                              ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""");

            DB.executeUpdate(
                    """
                              CREATE TABLE IF NOT EXISTS `mp_plots` (
                                `plotId` int(11) NOT NULL AUTO_INCREMENT,
                                `cityId` int(11) NOT NULL,
                                `cityName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `plotName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `plotOwner` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                `plotOwnerUUID` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                `plotPoints` text COLLATE utf8mb4_unicode_ci NOT NULL,
                                `plotBoundary` GEOMETRY NOT NULL,
                                `plotYMin` int(11) NOT NULL,
                                `plotYMax` int(11) NOT NULL,
                                `plotType` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                `plotIsForSale` tinyint(1) DEFAULT 0,
                                `plotKMarked` tinyint(1) DEFAULT 0,
                                `plotPrice` int(25) DEFAULT 0,
                                `plotRent` int(25) DEFAULT 0,
                                `plotPermsMembers` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `plotPermsOutsiders` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `plotCenter` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `plotFlags` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                `plotCreationDate` bigint(30) DEFAULT NULL,
                                PRIMARY KEY (`plotId`),
                                SPATIAL INDEX `idx_plotBoundary` (`plotBoundary`)
                              ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""");

            DB.executeUpdate(
                    """
                              CREATE TABLE IF NOT EXISTS `mp_plotperms` (
                                `plotId` int(11) NOT NULL,
                                `cityId` int(11) NOT NULL,
                                `plotPerms` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `playerUUID` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `playerName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                PRIMARY KEY (plotId,playerUUID)
                              ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""");
            DB.executeUpdate(
                    """
                              CREATE TABLE IF NOT EXISTS `mp_cityperms` (
                                `cityId` int(11) NOT NULL,
                                `cityPerms` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `playerUUID` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `playerName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                PRIMARY KEY (cityId,playerUUID)
                              ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""");

            DB.executeUpdate(
                    """
                              CREATE TABLE IF NOT EXISTS `mp_citygoes` (
                                `cityID` int(11) NOT NULL,
                                `goName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `goNickname` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                `goLocation` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `isOpen` tinyint(1) DEFAULT 1,
                                `accessLevel` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                `createDate` bigint(30) DEFAULT NULL,
                                 PRIMARY KEY (cityID,goName)
                              ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""");
            DB.executeUpdate(
                    """
                              CREATE TABLE IF NOT EXISTS `mp_citybans` (
                                `cityID` int(11) NOT NULL,
                                `playerUUID` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                `placeDate` bigint(30) DEFAULT NULL,
                                `length` bigint(30) DEFAULT NULL,
                                `reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                `placeUUID` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                 PRIMARY KEY (cityID,playerUUID)
                              ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;""");
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public static void addLogEntry(City city, String logEntry) {
        try {
            int cityId = city.getCityID();
            DB.executeInsert(
                    "INSERT INTO `mp_citylogs` (`cityId`, `dateTime`, `jsonLog`) VALUES ("
                            + cityId
                            + ", "
                            + DateUtil.getTimestamp()
                            + ", "
                            + Database.sqlString(logEntry)
                            + ");");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
