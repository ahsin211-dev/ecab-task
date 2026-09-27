package com.example.ridematching.service;

import com.example.ridematching.domain.Driver;
import com.example.ridematching.domain.DriverStatus;
import com.example.ridematching.domain.Location;
import com.example.ridematching.domain.Ride;
import com.example.ridematching.exception.DriverOnRideException;
import com.example.ridematching.exception.NoDriverAvailableException;
import com.example.ridematching.exception.RideAlreadyCompletedException;
import com.example.ridematching.exception.RiderHasActiveRideException;
import com.example.ridematching.support.InMemoryRideMatching;
import com.example.ridematching.support.Race;
import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These are the tests that prove the lock-free CAS design in Driver and Ride actually works.
 */
class RideServiceConcurrencyTest {

    private static final Location PICKUP = new Location(0, 0);

    private final InMemoryRideMatching app = new InMemoryRideMatching();
    private final RideService rideService = app.rides();
    private final DriverService driverService = app.drivers();

    /**
     * 50 riders book at once against 10 drivers: 10 rides succeed, all with different drivers, 40 get
     * NoDriverAvailableException.
     */
    @RepeatedTest(20)
    void fiftyRidersForTenDriversNeverShareADriver() {
        registerDrivers(10);

        Race.Result<Ride> race = Race.run(50, i -> requestRide("rider-" + i));

        assertThat(race.winners()).hasSize(10);
        assertThat(race.winners()).extracting(Ride::driverId).doesNotHaveDuplicates();
        assertThat(race.failures()).hasSize(40).hasOnlyElementsOfType(NoDriverAvailableException.class);
    }

    /**
     * Completes one ride 10 times at once: 1 succeeds, 9 get RideAlreadyCompletedException, the driver ends
     * AVAILABLE.
     */
    @RepeatedTest(20)
    void tenSimultaneousCompletionsOnlyOneSucceeds() {
        registerDrivers(1);
        String rideId = requestRide("rider-1").id();

        Race.Result<RideDetails> race = Race.run(10, i -> rideService.completeRide(rideId, "rider-1"));

        assertThat(race.winners()).hasSize(1);
        assertThat(race.failures()).hasSize(9).hasOnlyElementsOfType(RideAlreadyCompletedException.class);
        assertThat(app.statusOf("driver-0")).isEqualTo(DriverStatus.AVAILABLE);
    }

    /**
     * One rider books 10 times at once: 1 ride succeeds, 9 get RiderHasActiveRideException, exactly 1 driver is
     * ON_RIDE.
     */
    @RepeatedTest(20)
    void oneRiderBookingTenTimesAtOnceGetsOneRide() {
        registerDrivers(20);

        Race.Result<Ride> race = Race.run(10, i -> requestRide("rider-1"));

        assertThat(race.winners()).hasSize(1);
        assertThat(race.failures()).hasSize(9).hasOnlyElementsOfType(RiderHasActiveRideException.class);
        assertThat(driversOnRide()).as("drivers taken").isEqualTo(1);
    }

    private void registerDrivers(int count) {
        for (int i = 0; i < count; i++) {
            app.availableDriver("driver-" + i, i + 1, 0);
        }
    }

    private Ride requestRide(String riderId) {
        return rideService.requestRide(riderId, PICKUP).ride();
    }

    private long driversOnRide() {
        return app.driverRepository().findAll().stream()
                .map(Driver::currentState)
                .filter(state -> state.status() == DriverStatus.ON_RIDE)
                .count();
    }
}
