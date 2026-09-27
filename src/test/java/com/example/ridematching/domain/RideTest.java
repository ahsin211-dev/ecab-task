package com.example.ridematching.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RideTest {

    private static final Instant REQUESTED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant COMPLETED_AT = Instant.parse("2026-01-01T10:30:00Z");

    @Test
    void startsInProgress() {
        Ride ride = newRide();

        assertThat(ride.status()).isEqualTo(RideStatus.IN_PROGRESS);
        assertThat(ride.completedAt()).isNull();
    }

    @Test
    void completesInProgressRide() {
        Ride ride = newRide();

        assertThat(ride.markCompleted(COMPLETED_AT)).isTrue();
        assertThat(ride.status()).isEqualTo(RideStatus.COMPLETED);
        assertThat(ride.completedAt()).isEqualTo(COMPLETED_AT);
    }

    @Test
    void secondCompletionReturnsFalse() {
        Ride ride = newRide();
        ride.markCompleted(COMPLETED_AT);

        assertThat(ride.markCompleted(COMPLETED_AT.plusSeconds(60))).isFalse();
        assertThat(ride.completedAt()).isEqualTo(COMPLETED_AT);
    }

    private static Ride newRide() {
        return Ride.start("ride-1", "rider-1", "d1", new Location(0, 0), REQUESTED_AT);
    }
}
