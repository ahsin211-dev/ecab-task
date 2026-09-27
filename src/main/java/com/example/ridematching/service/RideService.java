package com.example.ridematching.service;

import com.example.ridematching.domain.Location;

public interface RideService {

    RideDetails requestRide(String riderId, Location pickup);

    RideDetails completeRide(String rideId, String riderId);

    RideDetails getRide(String rideId);
}
