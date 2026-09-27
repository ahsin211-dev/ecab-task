package com.example.ridematching.api.dto;

import com.example.ridematching.domain.Ride;
import com.example.ridematching.service.RideDetails;

import java.time.Instant;

public record RideResponse(
        String rideId,
        String status,
        String riderId,
        LocationDto pickup,
        DriverResponse driver,
        Instant requestedAt,
        Instant completedAt) {

    public static RideResponse from(RideDetails details) {
        Ride ride = details.ride();
        return new RideResponse(
                ride.id(),
                ride.status().name(),
                ride.riderId(),
                LocationDto.from(ride.pickup()),
                DriverResponse.from(ride.driverId(), details.driver()),
                ride.requestedAt(),
                ride.completedAt());
    }
}
