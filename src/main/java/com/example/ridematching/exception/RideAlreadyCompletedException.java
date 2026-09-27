package com.example.ridematching.exception;

public class RideAlreadyCompletedException extends RideMatchingException {

    public RideAlreadyCompletedException(String rideId) {
        super("Ride " + rideId + " is already completed");
    }
}
