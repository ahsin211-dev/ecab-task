package com.example.ridematching.service.impl;

import com.example.ridematching.domain.Driver;
import com.example.ridematching.domain.Location;
import com.example.ridematching.domain.Ride;
import com.example.ridematching.exception.*;
import com.example.ridematching.matching.DriverMatchingStrategy;
import com.example.ridematching.matching.RankedDriver;
import com.example.ridematching.repository.DriverRepository;
import com.example.ridematching.repository.RideRepository;
import com.example.ridematching.service.ActiveRiderRegistry;
import com.example.ridematching.service.IdGenerator;
import com.example.ridematching.service.RideDetails;
import com.example.ridematching.service.RideService;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class RideServiceImpl implements RideService {

    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    private final DriverMatchingStrategy matchingStrategy;
    private final ActiveRiderRegistry activeRiders;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public RideServiceImpl(DriverRepository driverRepository,
                           RideRepository rideRepository,
                           DriverMatchingStrategy matchingStrategy,
                           ActiveRiderRegistry activeRiders,
                           IdGenerator idGenerator,
                           Clock clock) {
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
        this.matchingStrategy = matchingStrategy;
        this.activeRiders = activeRiders;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Override
    public RideDetails requestRide(String riderId, Location pickup) {
        if (!activeRiders.tryRegister(riderId)) {
            throw new RiderHasActiveRideException(riderId);
        }
        try {
            Driver driver = reserveNearestDriver(pickup);
            return startRide(riderId, driver, pickup);
        } catch (RuntimeException e) {
            // Any failure before the ride is stored must free the rider, otherwise they are locked out forever.
            activeRiders.unregister(riderId);
            throw e;
        }
    }

    @Override
    public RideDetails completeRide(String rideId, String riderId) {
        Ride ride = findRide(rideId);
        if (!ride.isRequestedBy(riderId)) {
            throw new RideAccessDeniedException(rideId, riderId);
        }
        if (!ride.markCompleted(clock.instant())) {
            throw new RideAlreadyCompletedException(rideId);
        }

        // Only the thread that won markCompleted gets here, so the driver is released exactly once.
        Driver driver = findDriver(ride.driverId());
        driver.release();
        activeRiders.unregister(riderId);
        return new RideDetails(ride, driver.currentState());
    }

    @Override
    public RideDetails getRide(String rideId) {
        Ride ride = findRide(rideId);
        return new RideDetails(ride, findDriver(ride.driverId()).currentState());
    }

    private Driver reserveNearestDriver(Location pickup) {
        // The ranking is a snapshot. A candidate may be taken by another request before we get to it,
        // so walk down the list until a reservation actually sticks.
        for (RankedDriver candidate : matchingStrategy.rank(pickup, driverRepository.findAvailable(), Integer.MAX_VALUE)) {
            Driver driver = findDriver(candidate.driverId());
            if (driver.tryReserve()) {
                return driver;
            }
        }
        throw new NoDriverAvailableException();
    }

    private RideDetails startRide(String riderId, Driver driver, Location pickup) {
        try {
            Ride ride = Ride.start(idGenerator.nextId(), riderId, driver.id(), pickup, clock.instant());
            rideRepository.save(ride);
            return new RideDetails(ride, driver.currentState());
        } catch (RuntimeException e) {
            driver.release();
            throw e;
        }
    }

    private Ride findRide(String rideId) {
        return rideRepository.findById(rideId).orElseThrow(() -> new RideNotFoundException(rideId));
    }

    private Driver findDriver(String driverId) {
        return driverRepository.findById(driverId).orElseThrow(() -> new DriverNotFoundException(driverId));
    }
}
