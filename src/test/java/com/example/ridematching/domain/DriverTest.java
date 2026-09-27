package com.example.ridematching.domain;

import com.example.ridematching.exception.DriverOnRideException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriverTest {

    private static final Location ORIGIN = new Location(0, 0);

    @Test
    void reservesAvailableDriver() {
        Driver driver = Driver.register("d1", ORIGIN, true);

        assertThat(driver.tryReserve()).isTrue();
        assertThat(driver.currentState().status()).isEqualTo(DriverStatus.ON_RIDE);
    }

    @Test
    void doesNotReserveOfflineDriver() {
        Driver driver = Driver.register("d1", ORIGIN, false);

        assertThat(driver.tryReserve()).isFalse();
        assertThat(driver.currentState().status()).isEqualTo(DriverStatus.OFFLINE);
    }

    @Test
    void doesNotReserveDriverAlreadyOnRide() {
        Driver driver = Driver.register("d1", ORIGIN, true);
        driver.tryReserve();

        assertThat(driver.tryReserve()).isFalse();
    }

    @Test
    void releaseMakesDriverAvailableAgain() {
        Driver driver = Driver.register("d1", ORIGIN, true);
        driver.tryReserve();

        driver.release();

        assertThat(driver.currentState().status()).isEqualTo(DriverStatus.AVAILABLE);
    }

    @Test
    void releaseDoesNotChangeOfflineDriver() {
        Driver driver = Driver.register("d1", ORIGIN, false);

        driver.release();

        assertThat(driver.currentState().status()).isEqualTo(DriverStatus.OFFLINE);
    }

    @Test
    void updatesLocationWhileOnRide() {
        Driver driver = Driver.register("d1", ORIGIN, true);
        driver.tryReserve();

        DriverState state = driver.moveTo(new Location(3, 4));

        assertThat(state.location()).isEqualTo(new Location(3, 4));
        assertThat(state.status()).isEqualTo(DriverStatus.ON_RIDE);
    }

    @Test
    void rejectsAvailabilityChangeWhileOnRide() {
        Driver driver = Driver.register("d1", ORIGIN, true);
        driver.tryReserve();

        assertThatThrownBy(() -> driver.updateAvailability(new Location(1, 1), false))
                .isInstanceOf(DriverOnRideException.class);
        assertThatThrownBy(() -> driver.updateAvailability(new Location(1, 1), true))
                .isInstanceOf(DriverOnRideException.class);
        assertThat(driver.currentState()).isEqualTo(new DriverState(ORIGIN, DriverStatus.ON_RIDE));
    }

    @Test
    void togglesAvailabilityWhenNotOnRide() {
        Driver driver = Driver.register("d1", ORIGIN, true);

        DriverState offline = driver.updateAvailability(new Location(2, 2), false);

        assertThat(offline).isEqualTo(new DriverState(new Location(2, 2), DriverStatus.OFFLINE));
    }
}
