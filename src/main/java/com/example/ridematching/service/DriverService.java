package com.example.ridematching.service;

import com.example.ridematching.domain.DriverState;
import com.example.ridematching.domain.Location;
import com.example.ridematching.matching.RankedDriver;

import java.util.List;

public interface DriverService {

    int MIN_NEAREST_LIMIT = 1;
    int MAX_NEAREST_LIMIT = 100;

    /**
     * Registers the driver on first call, afterwards updates location and availability.
     */
    DriverState updateDriver(String driverId, Location location, boolean available);

    /**
     * Moves an existing driver without touching availability; allowed while on a ride.
     */
    DriverState updateLocation(String driverId, Location location);

    List<DriverDetails> findAll();

    List<RankedDriver> findNearestAvailable(Location origin, int limit);
}
