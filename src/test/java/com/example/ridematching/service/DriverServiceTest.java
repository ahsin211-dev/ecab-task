package com.example.ridematching.service;

import com.example.ridematching.domain.DriverState;
import com.example.ridematching.domain.DriverStatus;
import com.example.ridematching.domain.Location;
import com.example.ridematching.exception.DriverNotFoundException;
import com.example.ridematching.exception.DriverOnRideException;
import com.example.ridematching.matching.RankedDriver;
import com.example.ridematching.support.InMemoryRideMatching;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * How drivers join, move, change availability and are found.
 * Distance maths and tie-breaking are covered by NearestDriverMatchingStrategyTest.
 */
class DriverServiceTest {

    private static final Location ORIGIN = new Location(0, 0);

    private final InMemoryRideMatching app = new InMemoryRideMatching();
    private final DriverService driverService = app.drivers();

    @Nested
    class RegisteringAndUpdating {

        /** Registers a new driver as available and checks the returned location and status. */
        @Test
        void newDriverCanStartAvailable() {
            DriverState state = driverService.updateDriver("d1", new Location(1, 2), true);

            assertThat(state).isEqualTo(new DriverState(new Location(1, 2), DriverStatus.AVAILABLE));
        }

        /** Updates an existing driver by id and checks the state changed with no second driver added. */
        @Test
        void sameIdUpdatesTheExistingDriverInsteadOfAddingOne() {
            app.availableDriver("d1", 0, 0);

            DriverState state = driverService.updateDriver("d1", new Location(5, 5), false);

            assertThat(state).isEqualTo(new DriverState(new Location(5, 5), DriverStatus.OFFLINE));
            assertThat(driverService.findAll()).hasSize(1);
        }

        /** Tries to set a driver on a ride to offline and expects DriverOnRideException. */
        @Test
        void driverOnRideCannotChangeAvailability() {
            app.driverOnRide("d1", 0, 0);

            assertThatThrownBy(() -> driverService.updateDriver("d1", ORIGIN, false))
                    .isInstanceOf(DriverOnRideException.class);
        }

        /** Moves a driver who is on a ride and checks the location changes while status stays ON_RIDE. */
        @Test
        void driverOnRideCanStillMove() {
            app.driverOnRide("d1", 0, 0);

            DriverState state = driverService.updateLocation("d1", new Location(8, 8));

            assertThat(state).isEqualTo(new DriverState(new Location(8, 8), DriverStatus.ON_RIDE));
        }

        /** Moves a driver id that was never registered and expects DriverNotFoundException. */
        @Test
        void movingAnUnknownDriverFails() {
            assertThatThrownBy(() -> driverService.updateLocation("ghost", ORIGIN))
                    .isInstanceOf(DriverNotFoundException.class);
        }
    }

    @Nested
    class ListingAllDrivers {

        /** Lists available, on-ride and offline drivers and checks all appear, sorted by id. */
        @Test
        void includesEveryStatusSortedById() {
            app.offlineDriver("c-offline", 3, 4);
            app.availableDriver("a-available", 1, 2);
            app.driverOnRide("b-busy", 0, 0);

            assertThat(driverService.findAll())
                    .extracting(DriverDetails::driverId, driver -> driver.state().status())
                    .containsExactly(
                            tuple("a-available", DriverStatus.AVAILABLE),
                            tuple("b-busy", DriverStatus.ON_RIDE),
                            tuple("c-offline", DriverStatus.OFFLINE));
        }

        /** Takes the driver list, moves a driver afterwards, and checks the list still shows the old location. */
        @Test
        void returnedListDoesNotChangeWhenDriversMoveLater() {
            app.availableDriver("d1", 1, 2);
            List<DriverDetails> drivers = driverService.findAll();

            driverService.updateLocation("d1", new Location(9, 9));

            assertThat(drivers.getFirst().state().location()).isEqualTo(new Location(1, 2));
        }
    }

    @Nested
    class FindingNearestDrivers {

        /** Finds nearest drivers with limit 2 and checks only the two closest come back, nearest first. */
        @Test
        void returnsClosestFirstAndStopsAtLimit() {
            app.availableDriver("far", 9, 0);
            app.availableDriver("near", 1, 0);
            app.availableDriver("mid", 4, 0);

            assertThat(driverService.findNearestAvailable(ORIGIN, 2))
                    .extracting(RankedDriver::driverId)
                    .containsExactly("near", "mid");
        }

        /** Asks for nearest drivers with limit 0 and 101 and expects IllegalArgumentException for both. */
        @Test
        void rejectsLimitOutsideOneToOneHundred() {
            assertThatThrownBy(() -> driverService.findNearestAvailable(ORIGIN, 0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> driverService.findNearestAvailable(ORIGIN, 101))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
