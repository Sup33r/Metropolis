package live.supeer.metropolis.city;

import co.aikar.idb.DbRow;
import live.supeer.metropolis.Metropolis;
import lombok.Getter;

@Getter
public class Claim {
    public static Metropolis plugin;

    private final int claimId;
    private final String claimerUUID;
    private final String claimerName;
    private final String claimWorld;
    private final int xPosition;
    private final int zPosition;
    private final String cityName;
    private final long claimDate;
    private final boolean outpost;

    public Claim(DbRow data) {
        this.claimId = data.getInt("claimId");
        this.claimerUUID = data.getString("claimerUUID");
        this.claimerName = data.getString("claimerName");
        this.claimWorld = data.getString("world");
        this.xPosition = data.getInt("xPosition");
        this.zPosition = data.getInt("zPosition");
        this.cityName = data.getString("cityName");
        this.claimDate = data.getInt("claimDate");
        this.outpost = data.get("outpost");
    }
}
