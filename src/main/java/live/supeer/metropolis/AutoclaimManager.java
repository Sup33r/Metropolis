package live.supeer.metropolis;

import live.supeer.metropolis.city.City;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutoclaimManager {
    private static final Map<UUID, AutoclaimInfo> autoclaimers = new HashMap<>();

    public static void startAutoclaim(Player player, City city, int count) {
        autoclaimers.put(player.getUniqueId(), new AutoclaimInfo(city, count));
    }

    public static void stopAutoclaim(Player player) {
        autoclaimers.remove(player.getUniqueId());
    }

    public static boolean isAutoclaiming(Player player) {
        return autoclaimers.containsKey(player.getUniqueId());
    }

    public static AutoclaimInfo getAutoclaimInfo(Player player) {
        return autoclaimers.get(player.getUniqueId());
    }

    public static void decrementAutoclaimCount(Player player) {
        AutoclaimInfo info = autoclaimers.get(player.getUniqueId());
        if (info != null) {
            info.decrementCount();
            if (info.getCount() <= 0) {
                stopAutoclaim(player);
            }
        }
    }

    @Getter
    static class AutoclaimInfo {
        private final City city;
        private int count;

        AutoclaimInfo(City city, int count) {
            this.city = city;
            this.count = count;
        }

        public void decrementCount() {
            count--;
        }
    }
}
