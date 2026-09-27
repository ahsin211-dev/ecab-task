package com.example.ridematching.matching.impl;

import com.example.ridematching.domain.Driver;
import com.example.ridematching.domain.Location;
import com.example.ridematching.matching.DriverMatchingStrategy;
import com.example.ridematching.matching.RankedDriver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NearestDriverMatchingStrategyTest {

    private static final Location ORIGIN = new Location(0, 0);

    private final DriverMatchingStrategy strategy =
            new NearestDriverMatchingStrategy(new EuclideanDistanceCalculator());

    @Test
    void ordersDriversByAscendingDistance() {
        List<Driver> drivers = List.of(
                available("far", 10, 0),
                available("near", 1, 0),
                available("middle", 0, 5));

        List<RankedDriver> ranked = strategy.rank(ORIGIN, drivers, 10);

        assertThat(ranked).extracting(RankedDriver::driverId).containsExactly("near", "middle", "far");
        assertThat(ranked).extracting(RankedDriver::distance).containsExactly(1.0, 5.0, 10.0);
    }

    @Test
    void breaksTiesByDriverId() {
        List<Driver> drivers = List.of(
                available("c", 0, 2),
                available("a", 2, 0),
                available("b", -2, 0));

        List<RankedDriver> ranked = strategy.rank(ORIGIN, drivers, 10);

        assertThat(ranked).extracting(RankedDriver::driverId).containsExactly("a", "b", "c");
    }

    @Test
    void respectsLimit() {
        List<Driver> drivers = List.of(
                available("d1", 1, 0),
                available("d2", 2, 0),
                available("d3", 3, 0));

        assertThat(strategy.rank(ORIGIN, drivers, 2))
                .extracting(RankedDriver::driverId)
                .containsExactly("d1", "d2");
    }

    @Test
    void returnsAllWhenLimitExceedsDriverCount() {
        List<Driver> drivers = List.of(available("d1", 1, 0), available("d2", 2, 0));

        assertThat(strategy.rank(ORIGIN, drivers, Integer.MAX_VALUE)).hasSize(2);
    }

    @Test
    void returnsEmptyListWhenNoDrivers() {
        assertThat(strategy.rank(ORIGIN, List.of(), 5)).isEmpty();
    }

    private static Driver available(String id, double x, double y) {
        return Driver.register(id, new Location(x, y), true);
    }
}
