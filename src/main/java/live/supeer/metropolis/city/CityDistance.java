package live.supeer.metropolis.city;

import lombok.Getter;

@Getter
public class CityDistance {
    private final City city;
    private final int distance;

    public CityDistance(City city, int distance) {
        this.city = city;
        this.distance = distance;
    }

}
