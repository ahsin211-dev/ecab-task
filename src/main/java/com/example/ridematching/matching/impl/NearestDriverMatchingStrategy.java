package com.example.ridematching.matching.impl;

import com.example.ridematching.domain.Driver;
import com.example.ridematching.domain.DriverState;
import com.example.ridematching.domain.Location;
import com.example.ridematching.matching.DistanceCalculator;
import com.example.ridematching.matching.DriverMatchingStrategy;
import com.example.ridematching.matching.RankedDriver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Component
public class NearestDriverMatchingStrategy implements DriverMatchingStrategy {

    private static final Comparator<RankedDriver> NEAREST_FIRST = Comparator
            .comparingDouble(RankedDriver::distance)
            .thenComparing(RankedDriver::driverId);

    private final DistanceCalculator distanceCalculator;

    public NearestDriverMatchingStrategy(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    @Override
    public List<RankedDriver> rank(Location origin, Collection<Driver> drivers, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        List<RankedDriver> candidates = new ArrayList<>(drivers.size());
        for (Driver driver : drivers) {
            // One read per driver. Sorting on live state would let a concurrent move change a
            // driver's distance mid-sort and break the comparator's contract.
            DriverState snapshot = driver.currentState();
            if (snapshot.isAvailable()) {
                double distance = distanceCalculator.distance(origin, snapshot.location());
                candidates.add(new RankedDriver(driver.id(), snapshot, distance));
            }
        }

        // A full sort is O(n log n) and plenty for an in-memory fleet; a bounded heap only
        // pays off once limit is tiny compared to a very large n.
        candidates.sort(NEAREST_FIRST);
        return candidates.size() <= limit ? candidates : List.copyOf(candidates.subList(0, limit));
    }
}
