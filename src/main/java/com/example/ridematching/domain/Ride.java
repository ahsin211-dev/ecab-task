package com.example.ridematching.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class Ride {

    private final String id;
    private final String riderId;
    private final String driverId;
    private final Location pickup;
    private final Instant requestedAt;
    private final AtomicReference<RideStatus> status = new AtomicReference<>(RideStatus.IN_PROGRESS);
    private volatile Instant completedAt;

    private Ride(String id, String riderId, String driverId, Location pickup, Instant requestedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.riderId = Objects.requireNonNull(riderId, "riderId");
        this.driverId = Objects.requireNonNull(driverId, "driverId");
        this.pickup = Objects.requireNonNull(pickup, "pickup");
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
    }

    public static Ride start(String id, String riderId, String driverId, Location pickup, Instant requestedAt) {
        return new Ride(id, riderId, driverId, pickup, requestedAt);
    }

    public boolean markCompleted(Instant at) {
        Objects.requireNonNull(at, "at");
        if (!status.compareAndSet(RideStatus.IN_PROGRESS, RideStatus.COMPLETED)) {
            return false;
        }
        // Only the CAS winner writes this. A reader can briefly see COMPLETED with a null
        // completedAt; that is acceptable for a response snapshot.
        completedAt = at;
        return true;
    }

    public boolean isRequestedBy(String candidateRiderId) {
        return riderId.equals(candidateRiderId);
    }

    public String id() {
        return id;
    }

    public String riderId() {
        return riderId;
    }

    public String driverId() {
        return driverId;
    }

    public Location pickup() {
        return pickup;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    public RideStatus status() {
        return status.get();
    }

    public Instant completedAt() {
        return completedAt;
    }
}
