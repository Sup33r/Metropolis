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
    public static Metropolis plugin;

    private final int claimId;
    private final String claimerUUID;
    private final String claimerName;
    private final World claimWorld;
    private final int xPosition;
    private final int zPosition;
    private final String cityName;
    private final long claimDate;
    private final boolean outpost;
    private final City city;
    private Point point;

    public Claim(DbRow data) {
        this.claimId = data.getInt("claimId");
        this.claimerUUID = data.getString("claimerUUID");
        this.claimerName = data.getString("claimerName");
        this.claimWorld = plugin.getServer().getWorld(data.getString("claimWorld"));
        this.xPosition = data.getInt("xPosition");
        this.zPosition = data.getInt("zPosition");
        this.cityName = data.getString("cityName");
        this.claimDate = data.getLong("claimDate");
        this.outpost = data.get("outpost");
        if (CityDatabase.getCity(cityName).isPresent()) {
            this.city = CityDatabase.getCity(cityName).get();
        } else {
            this.city = null;
        }
        GeometryFactory factory = new GeometryFactory();
        this.point = factory.createPoint(new Coordinate(this.xPosition, this.zPosition));
    }
}
