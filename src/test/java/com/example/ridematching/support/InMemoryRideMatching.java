package com.example.ridematching.support;

import com.example.ridematching.domain.DriverStatus;
import com.example.ridematching.domain.Location;
import com.example.ridematching.matching.impl.EuclideanDistanceCalculator;
import com.example.ridematching.matching.impl.NearestDriverMatchingStrategy;
import com.example.ridematching.repository.DriverRepository;
import com.example.ridematching.repository.impl.InMemoryDriverRepository;
import com.example.ridematching.repository.impl.InMemoryRideRepository;
import com.example.ridematching.service.ActiveRiderRegistry;
import com.example.ridematching.service.DriverService;
import com.example.ridematching.service.RideService;
import com.example.ridematching.service.impl.DriverServiceImpl;
import com.example.ridematching.service.impl.RideServiceImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * The real services wired to the real in-memory repositories, plus short helpers
 * so tests can say "an available driver at (1, 0)" instead of repeating setup code.
 */
public final class InMemoryRideMatching {

    public static final Instant NOW = Instant.parse("2026-01-01T09:00:00Z");

    private final DriverRepository driverRepository = new InMemoryDriverRepository();
    private final NearestDriverMatchingStrategy matching =
            new NearestDriverMatchingStrategy(new EuclideanDistanceCalculator());
    private final DriverService driverService = new DriverServiceImpl(driverRepository, matching);
    private final RideService rideService = new RideServiceImpl(
            driverRepository,
            new InMemoryRideRepository(),
            matching,
            new ActiveRiderRegistry(),
            new SequentialIdGenerator(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    public DriverService drivers() {
        return driverService;
    }

    public RideService rides() {
        return rideService;
    }

    public DriverRepository driverRepository() {
        return driverRepository;
    }

    public void availableDriver(String driverId, double x, double y) {
        driverService.updateDriver(driverId, new Location(x, y), true);
    }

    public void offlineDriver(String driverId, double x, double y) {
        driverService.updateDriver(driverId, new Location(x, y), false);
    }

    public void driverOnRide(String driverId, double x, double y) {
        availableDriver(driverId, x, y);
        driverRepository.findById(driverId).orElseThrow().tryReserve();
    }

    public DriverStatus statusOf(String driverId) {
        return driverRepository.findById(driverId).orElseThrow().currentState().status();
    }
}
