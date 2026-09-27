package com.example.ridematching.exception;

public class DriverOnRideException extends RideMatchingException {

    public DriverOnRideException(String driverId) {
        super("Driver " + driverId + " is on a ride; availability cannot change until the ride is completed");
    }
}
