package live.supeer.metropolis.city;

import co.aikar.idb.DbRow;
import live.supeer.metropolis.Metropolis;
import lombok.Getter;
import org.bukkit.World;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

@Getter
public class Claim {
    private final int claimId;
    private final String claimerUUID;
    private final String claimerName;
    private final World claimWorld;
    private final int xPosition;
    private final int zPosition;
    private final int cityId;
    private final long claimDate;
    private final boolean outpost;
    private final City city;
    public Claim(DbRow data) {
        this.claimId = data.getInt("claimId");
        this.claimerUUID = data.getString("claimerUUID");
        this.claimerName = data.getString("claimerName");
        this.claimWorld = Metropolis.getInstance().getServer().getWorld(data.getString("world"));
        this.xPosition = data.getInt("xPosition");
        this.zPosition = data.getInt("zPosition");
        this.cityId = data.getInt("cityId");
        this.claimDate = data.getLong("claimDate");
        this.outpost = data.get("outpost");
        if (CityDatabase.getCity(cityId).isPresent()) {
            this.city = CityDatabase.getCity(cityId).get();
        } else {
            this.city = null;
        }
    }
}
