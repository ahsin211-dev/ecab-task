package com.example.ridematching.exception;

public class RiderHasActiveRideException extends RideMatchingException {

    public RiderHasActiveRideException(String riderId) {
        super("Rider " + riderId + " already has a ride in progress");
    }
}
