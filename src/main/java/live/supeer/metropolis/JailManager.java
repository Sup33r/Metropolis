package live.supeer.metropolis;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import live.supeer.apied.ApiedAPI;
import live.supeer.apied.MPlayer;
import live.supeer.apied.MPlayerManager;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.utils.DateUtil;
import live.supeer.metropolis.utils.LocationUtil;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class JailManager {

    public static Cell createCell(Plot plot, Location location) {
        try {
            DB.executeUpdate("INSERT INTO mp_cells (plotId, location) VALUES (?, ?)",
                    plot.getPlotId(), LocationUtil.locationToString(location));
            return new Cell(DB.getFirstRow("SELECT * FROM mp_cells WHERE plotId = ? AND location = ?",
                    plot.getPlotId(), LocationUtil.locationToString(location)));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void UpdateCellSign(Cell cell, Location signLocation, Side side) {
        cell.setSignLocation(signLocation);
        cell.setSignSide(side);
    }

    public static Cell getRandomEmptyCell() {
        try {
            return new Cell(DB.getFirstRow("SELECT * FROM mp_cells WHERE prisonerUUID IS NULL ORDER BY RAND() LIMIT 1"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Cell> getCellsForPlot(Plot plot) {
        try {
            List<Cell> cells = new ArrayList<>();
            List<DbRow> rows = DB.getResults("SELECT * FROM mp_cells WHERE plotId = ?", plot.getPlotId());
            for (DbRow row : rows) {
                cells.add(new Cell(row));
            }
            return cells;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getOccupiedCellsCount(Plot plot) {
        try {
            List<DbRow> rows = DB.getResults("SELECT * FROM mp_cells WHERE plotId = ? AND prisonerUUID IS NOT NULL", plot.getPlotId());
            int count = 0;
            for (DbRow row : rows) {
                if (!banHasExpired(UUID.fromString(row.getString("prisonerUUID")))) {
                    count++;
                }
            }
            return count;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean hasCell(String prisonerUUID) {
        try {
            return DB.getFirstRow("SELECT * FROM mp_cells WHERE prisonerUUID = ?", prisonerUUID) != null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Cell getCellForPrisoner(String prisonerUUID) {
        try {
            return new Cell(DB.getFirstRow("SELECT * FROM mp_cells WHERE prisonerUUID = ?", prisonerUUID));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void releasePlayerFromCell(String prisonerUUID) {
        try {
            DB.executeUpdate("UPDATE mp_cells SET prisonerUUID = NULL WHERE prisonerUUID = ?", prisonerUUID);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteCell(Cell cell) {
        try {
            DB.executeUpdate("DELETE FROM mp_cells WHERE cellId = ?", cell.getCellId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteCellsForPlot(Plot plot) {
        try {
            DB.executeUpdate("DELETE FROM mp_cells WHERE plotId = ?", plot.getPlotId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Cell getCell(int id) {
        try {
            return new Cell(DB.getFirstRow("SELECT * FROM mp_cells WHERE cellId = ?", id));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean cellExists(int id) {
        try {
            return DB.getFirstRow("SELECT * FROM mp_cells WHERE cellId = ?", id) != null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void displaySign(Cell cell) {
        if (cell.getPrisonerUUID() == null) {
            displaySignEmptyCell(cell);
        } else {
            displayOccupiedCell(cell);
        }
    }

    public static void displaySignEmptyCell(Cell cell) {
        if (cell.getSignLocation() == null) {
            return;
        }
        Sign sign = (Sign) cell.getSignLocation().getBlock().getState();
        Side side = cell.getSignSide();
        sign.getSide(side).line(0, Objects.requireNonNull(Metropolis.getMessageComponent("messages.cell.sign.empty.row0")));
        sign.getSide(side).line(1, Objects.requireNonNull(Metropolis.getMessageComponent("messages.cell.sign.empty.row1")));
        sign.getSide(side).line(2, Objects.requireNonNull(Metropolis.getMessageComponent("messages.cell.sign.empty.row2", "%id%", String.valueOf(cell.getCellId()))));
        sign.getSide(side).line(3, Objects.requireNonNull(Metropolis.getMessageComponent("messages.cell.sign.empty.row3")));
        sign.setWaxed(true);
        sign.update();
    }

    public static void displayOccupiedCell(Cell cell) {
        if (cell.getSignLocation() == null) {
            return;
        }
        Sign sign = (Sign) cell.getSignLocation().getBlock().getState();
        Side side = cell.getSignSide();
        sign.getSide(side).line(0, Objects.requireNonNull(Metropolis.getMessageComponent("messages.cell.sign.occupied.row0")));
        sign.getSide(side).line(1, Objects.requireNonNull(Metropolis.getMessageComponent("messages.cell.sign.occupied.row1", "%playername%", ApiedAPI.getPlayer(UUID.fromString(cell.getPrisonerUUID())).getName())));
        sign.getSide(side).line(2, Objects.requireNonNull(Metropolis.getMessageComponent("messages.cell.sign.occupied.row2")));
        sign.getSide(side).line(3, Objects.requireNonNull(Metropolis.getMessageComponent("messages.cell.sign.occupied.row3")));
        sign.setWaxed(true);
        sign.update();
    }

    public static void sendJailMessage(Player player) {
        if (!hasCell(player.getUniqueId().toString())) {
            return;
        }
        long expiry = MPlayerManager.getBanLength(player.getUniqueId());
        String reason = MPlayerManager.getBanReason(player.getUniqueId());
        Cell cell = getCellForPrisoner(player.getUniqueId().toString());
        Metropolis.sendMessage(player, "messages.cell.jailMessage", "%player%", player.getName(), "%jail%", cell.getJailPlot().getPlotName(), "%city%", cell.getJailPlot().getCity().getCityName(), "%expiry%", DateUtil.niceDate(expiry), "%reason%", reason);
    }

    public static boolean signAlreadyExists(Location location) {
        try {
            return DB.getFirstRow("SELECT * FROM mp_cells WHERE signLocation = ?", LocationUtil.locationToString(location)) != null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Cell> getAllCells() {
        try {
            List<Cell> cells = new ArrayList<>();
            List<DbRow> rows = DB.getResults("SELECT * FROM mp_cells");
            for (DbRow row : rows) {
                cells.add(new Cell(row));
            }
            return cells;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void handleUnban(UUID playerUUID) {
        if (hasCell(playerUUID.toString())) {
            MPlayer player = ApiedAPI.getPlayer(playerUUID);
            if (player == null) {
                return;
            }
            player.setBanned(false);
            Cell cell = getCellForPrisoner(playerUUID.toString());
            releasePlayerFromCell(playerUUID.toString());
            displaySign(cell);
        }
    }

    public static boolean banHasExpired(UUID playerUUID) {
        if (MPlayerManager.getBanLength(playerUUID) < DateUtil.getTimestamp() || !MPlayerManager.isBanned(playerUUID)) {
            handleUnban(playerUUID);
            return true;
        }
        return false;
    }

    public static boolean banExpiresSoon(UUID playerUUID) {
        Metropolis.getInstance().logger.info("Checking if ban expires soon for " + playerUUID);
        if (!MPlayerManager.isBanned(playerUUID)) {
            return false;
        }
        Metropolis.getInstance().logger.info(MPlayerManager.getBanLength(playerUUID) + " - " + DateUtil.getTimestamp() + " < 86400");
        //time left is less than 24 hours
        return MPlayerManager.getBanLength(playerUUID) - DateUtil.getTimestamp() < 86400;
    }

    public static void compensateCity(City city) {
        city.addCityBalance(Metropolis.configuration.getPrisonerPayback());
        Database.addLogEntry(city, "{ \"type\": \"cityBank\", \"subtype\": \"jailCompensation\", \"balance\": " + Metropolis.configuration.getPrisonerPayback() + "}");
    }
}
