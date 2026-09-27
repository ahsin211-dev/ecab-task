package com.example.ridematching.exception;

public class RideAccessDeniedException extends RideMatchingException {

    public RideAccessDeniedException(String rideId, String riderId) {
        super("Rider " + riderId + " did not request ride " + rideId);
    }
}
