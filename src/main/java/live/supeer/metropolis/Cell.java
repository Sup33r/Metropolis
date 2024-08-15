package live.supeer.metropolis;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.plot.PlotDatabase;
import live.supeer.metropolis.utils.LocationUtil;
import live.supeer.metropolis.utils.Utilities;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.block.sign.Side;

@Getter
public class Cell {
    public static Metropolis plugin;

    private int cellId;
    private Plot jailPlot;
    private Location location;
    private String prisonerUUID;
    private Location signLocation;
    private Side signSide;

    public Cell(DbRow data) {
        this.cellId = data.getInt("cellId");
        this.jailPlot = PlotDatabase.getPlot(data.getInt("plotId"));
        this.location = LocationUtil.stringToLocation(data.getString("location"));
        this.prisonerUUID = data.getString("prisonerUUID") != null ? data.getString("prisonerUUID") : null;
        this.signLocation = data.getString("signLocation") != null ? LocationUtil.stringToLocation(data.getString("signLocation")) : null;
        this.signSide = Side.valueOf(data.getString("signSide"));
    }

    public void setSignSide(Side side) {
        try {
            DB.executeUpdate("UPDATE mp_cells SET signSide = ? WHERE cellId = ?", side.toString(), this.cellId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setSignLocation(Location location) {
        try {
            this.signLocation = location;
            DB.executeUpdate("UPDATE mp_cells SET signLocation = ? WHERE cellId = ?", LocationUtil.locationToString(location), this.cellId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setLocation(Location location) {
        try {
            this.location = location;
            DB.executeUpdate("UPDATE mp_cells SET location = ? WHERE cellId = ?", LocationUtil.locationToString(location), this.cellId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setPrisonerUUID(String prisonerUUID) {
        try {
            this.prisonerUUID = prisonerUUID;
            DB.executeUpdate("UPDATE mp_cells SET prisonerUUID = ? WHERE cellId = ?", prisonerUUID, this.cellId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void removePrisoner() {
        try {
            this.prisonerUUID = null;
            DB.executeUpdate("UPDATE mp_cells SET prisonerUUID = NULL WHERE cellId = ?", this.cellId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void delete() {
        try {
            DB.executeUpdate("DELETE FROM mp_cells WHERE cellId = ?", this.cellId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
