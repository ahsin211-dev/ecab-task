package com.example.ridematching.exception;

public class RideNotFoundException extends RideMatchingException {

    public RideNotFoundException(String rideId) {
        super("Ride " + rideId + " was not found");
    }
}
