package com.example.ridematching.exception;

public class DriverNotFoundException extends RideMatchingException {

    public DriverNotFoundException(String driverId) {
        super("Driver " + driverId + " was not found");
    }
}
