package com.example.ridematching.domain;

import com.example.ridematching.exception.DriverOnRideException;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class Driver {

    private final String id;
    private final AtomicReference<DriverState> state;

    public Driver(String id, DriverState initialState) {
        this.id = Objects.requireNonNull(id, "id");
        this.state = new AtomicReference<>(Objects.requireNonNull(initialState, "initialState"));
    }

    public static Driver register(String id, Location location, boolean available) {
        return new Driver(id, new DriverState(location, statusFor(available)));
    }

    private static DriverStatus statusFor(boolean available) {
        return available ? DriverStatus.AVAILABLE : DriverStatus.OFFLINE;
    }

    public String id() {
        return id;
    }

    public DriverState currentState() {
        return state.get();
    }

    public boolean tryReserve() {
        while (true) {
            DriverState current = state.get();
            if (!current.isAvailable()) {
                return false;
            }
            if (state.compareAndSet(current, current.withStatus(DriverStatus.ON_RIDE))) {
                return true;
            }
            // CAS can fail because the driver just moved, not only because someone else took them.
            // Loop and re-check instead of giving up on a driver who is still available.
        }
    }

    public void release() {
        state.updateAndGet(current -> current.status() == DriverStatus.ON_RIDE
                ? current.withStatus(DriverStatus.AVAILABLE)
                : current);
    }

    public DriverState moveTo(Location location) {
        Objects.requireNonNull(location, "location");
        return state.updateAndGet(current -> current.withLocation(location));
    }

    public DriverState updateAvailability(Location location, boolean available) {
        Objects.requireNonNull(location, "location");
        DriverState requested = new DriverState(location, statusFor(available));
        // updateAndGet may run this lambda several times under contention, so it must only read and throw.
        return state.updateAndGet(current -> {
            if (current.status() == DriverStatus.ON_RIDE) {
                throw new DriverOnRideException(id);
            }
            return requested;
        });
    }

    @Override
    public String toString() {
        return "Driver[" + id + ", " + state.get() + "]";
    }
}
