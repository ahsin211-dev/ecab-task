package com.example.ridematching.service;

import com.example.ridematching.domain.DriverStatus;
import com.example.ridematching.domain.Location;
import com.example.ridematching.domain.Ride;
import com.example.ridematching.domain.RideStatus;
import com.example.ridematching.exception.NoDriverAvailableException;
import com.example.ridematching.exception.RideAccessDeniedException;
import com.example.ridematching.exception.RideAlreadyCompletedException;
import com.example.ridematching.exception.RideNotFoundException;
import com.example.ridematching.exception.RiderHasActiveRideException;
import com.example.ridematching.support.InMemoryRideMatching;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.example.ridematching.support.InMemoryRideMatching.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideServiceTest {

    private static final Location PICKUP = new Location(0, 0);

    private final InMemoryRideMatching app = new InMemoryRideMatching();
    private final RideService rideService = app.rides();

    private Ride requestRide(String riderId) {
        return rideService.requestRide(riderId, PICKUP).ride();
    }

    @Nested
    class RequestingARide {

        /** Books a ride with a near and a far driver and checks the near one is assigned. */
        @Test
        void givesTheNearestAvailableDriver() {
            app.availableDriver("far", 10, 0);
            app.availableDriver("near", 1, 1);

            assertThat(requestRide("rider-1").driverId()).isEqualTo("near");
        }

        /** Books two rides in a row and checks the second rider gets the next closest driver. */
        @Test
        void nextRiderGetsTheNextNearestDriver() {
            app.availableDriver("near", 1, 0);
            app.availableDriver("next", 2, 0);

            requestRide("rider-1");

            assertThat(requestRide("rider-2").driverId()).isEqualTo("next");
        }

        /** Books a ride with a close offline driver and a far online one and checks the online one is assigned. */
        @Test
        void skipsOfflineDriversEvenWhenCloser() {
            app.offlineDriver("offline", 1, 0);
            app.availableDriver("online", 50, 0);

            assertThat(requestRide("rider-1").driverId()).isEqualTo("online");
        }

        /** Books a ride and checks every field of the returned ride and driver state. */
        @Test
        void returnsTheNewRideAndItsDriver() {
            app.availableDriver("d1", 3, 4);

            RideDetails details = rideService.requestRide("rider-1", PICKUP);

            assertThat(details.ride().id()).isEqualTo("ride-1");
            assertThat(details.ride().riderId()).isEqualTo("rider-1");
            assertThat(details.ride().pickup()).isEqualTo(PICKUP);
            assertThat(details.ride().status()).isEqualTo(RideStatus.IN_PROGRESS);
            assertThat(details.ride().requestedAt()).isEqualTo(NOW);
            assertThat(details.driver().location()).isEqualTo(new Location(3, 4));
            assertThat(details.driver().status()).isEqualTo(DriverStatus.ON_RIDE);
        }

        /** Books with no drivers (fails), adds a driver, books again with the same rider and checks it succeeds. */
        @Test
        void riderCanTryAgainAfterNoDriverWasAvailable() {
            assertThatThrownBy(() -> requestRide("rider-1"))
                    .isInstanceOf(NoDriverAvailableException.class);
            app.availableDriver("d1", 1, 0);

            assertThat(requestRide("rider-1").driverId()).isEqualTo("d1");
        }

        /**
         * Books a second ride for a rider already on a ride, expects RiderHasActiveRideException and checks no
         * extra driver was taken.
         */
        @Test
        void riderCannotHaveTwoRidesAtOnce() {
            app.availableDriver("d1", 1, 0);
            app.availableDriver("d2", 2, 0);
            requestRide("rider-1");

            assertThatThrownBy(() -> requestRide("rider-1"))
                    .isInstanceOf(RiderHasActiveRideException.class);
            assertThat(app.statusOf("d2")).as("second driver was not taken").isEqualTo(DriverStatus.AVAILABLE);
        }
    }

    @Nested
    class CompletingARide {

        private String rideId;

        @BeforeEach
        void bookRideForRider1WithDriverD1() {
            app.availableDriver("d1", 1, 0);
            rideId = requestRide("rider-1").id();
        }

        /** Completes the ride and checks it is COMPLETED and driver d1 is AVAILABLE again. */
        @Test
        void completesRideAndFreesTheDriver() {
            Ride ride = rideService.completeRide(rideId, "rider-1").ride();

            assertThat(ride.status()).isEqualTo(RideStatus.COMPLETED);
            assertThat(app.statusOf("d1")).isEqualTo(DriverStatus.AVAILABLE);
        }

        /** Completes the ride, then books again with the same rider and checks a new ride starts. */
        @Test
        void riderCanBookAgainAfterCompleting() {
            rideService.completeRide(rideId, "rider-1");

            assertThat(requestRide("rider-1").status()).isEqualTo(RideStatus.IN_PROGRESS);
        }

        /** Completes the ride as another rider, expects RideAccessDeniedException and d1 stays ON_RIDE. */
        @Test
        void otherRiderCannotComplete() {
            assertThatThrownBy(() -> rideService.completeRide(rideId, "rider-2"))
                    .isInstanceOf(RideAccessDeniedException.class);
            assertThat(app.statusOf("d1")).isEqualTo(DriverStatus.ON_RIDE);
        }

        /** Completes the same ride twice and expects RideAlreadyCompletedException the second time. */
        @Test
        void cannotCompleteTwice() {
            rideService.completeRide(rideId, "rider-1");

            assertThatThrownBy(() -> rideService.completeRide(rideId, "rider-1"))
                    .isInstanceOf(RideAlreadyCompletedException.class);
        }

        /** Completes a ride id that does not exist and expects RideNotFoundException. */
        @Test
        void unknownRideIsNotFound() {
            assertThatThrownBy(() -> rideService.completeRide("no-such-ride", "rider-1"))
                    .isInstanceOf(RideNotFoundException.class);
        }
    }

    /** Books a ride, looks it up by id and checks the assigned driver. */
    @Test
    void looksUpAnExistingRide() {
        app.availableDriver("d1", 1, 0);
        String rideId = requestRide("rider-1").id();

        assertThat(rideService.getRide(rideId).ride().driverId()).isEqualTo("d1");
    }
}
