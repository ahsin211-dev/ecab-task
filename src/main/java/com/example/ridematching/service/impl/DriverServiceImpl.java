package com.example.ridematching.service.impl;

import com.example.ridematching.domain.Driver;
import com.example.ridematching.domain.DriverState;
import com.example.ridematching.domain.Location;
import com.example.ridematching.exception.DriverNotFoundException;
import com.example.ridematching.matching.DriverMatchingStrategy;
import com.example.ridematching.matching.RankedDriver;
import com.example.ridematching.repository.DriverRepository;
import com.example.ridematching.service.DriverService;
import com.example.ridematching.service.DriverDetails;
import java.util.Comparator;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final DriverMatchingStrategy matchingStrategy;

    public DriverServiceImpl(DriverRepository driverRepository, DriverMatchingStrategy matchingStrategy) {
        this.driverRepository = driverRepository;
        this.matchingStrategy = matchingStrategy;
    }

    @Override
    public DriverState updateDriver(String driverId, Location location, boolean available) {
        Driver candidate = Driver.register(driverId, location, available);
        Driver driver = driverRepository.computeIfAbsent(driverId, id -> candidate);
        if (driver == candidate) {
            return candidate.currentState();
        }
        return driver.updateAvailability(location, available);
    }

    @Override
    public DriverState updateLocation(String driverId, Location location) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new DriverNotFoundException(driverId));
        return driver.moveTo(location);
    }

    @Override
    public List<DriverDetails> findAll() {
        return driverRepository.findAll().stream()
                .map(driver -> new DriverDetails(driver.id(), driver.currentState()))
                .sorted(Comparator.comparing(DriverDetails::driverId))
                .toList();
    }

    @Override
    public List<RankedDriver> findNearestAvailable(Location origin, int limit) {
        if (limit < MIN_NEAREST_LIMIT || limit > MAX_NEAREST_LIMIT) {
            throw new IllegalArgumentException(
                    "limit must be between " + MIN_NEAREST_LIMIT + " and " + MAX_NEAREST_LIMIT + ", got " + limit);
        }
        return matchingStrategy.rank(origin, driverRepository.findAvailable(), limit);
    }
}
