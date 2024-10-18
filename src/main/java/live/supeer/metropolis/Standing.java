package live.supeer.metropolis;

import lombok.Getter;

import java.util.UUID;

@Getter
public class Standing {
    private final int plotId;
    private final UUID playerUUID;
    private int count;

    public Standing(int plotId, UUID playerUUID, int count) {
        this.plotId = plotId;
        this.playerUUID = playerUUID;
        this.count = count;
    }

    public void incrementCount() {
        count++;
    }
}
