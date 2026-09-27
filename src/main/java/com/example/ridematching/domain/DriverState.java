package com.example.ridematching.domain;

import java.util.Objects;

public record DriverState(Location location, DriverStatus status) {

    public DriverState {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(status, "status");
    }

    public DriverState withStatus(DriverStatus newStatus) {
        return new DriverState(location, newStatus);
    }

    public DriverState withLocation(Location newLocation) {
        return new DriverState(newLocation, status);
    }

    public boolean isAvailable() {
        return status == DriverStatus.AVAILABLE;
    }
}
