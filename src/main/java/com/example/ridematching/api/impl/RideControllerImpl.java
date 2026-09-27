package com.example.ridematching.api.impl;

import com.example.ridematching.api.RideController;
import com.example.ridematching.api.dto.CompleteRideRequest;
import com.example.ridematching.api.dto.RideRequest;
import com.example.ridematching.api.dto.RideResponse;
import com.example.ridematching.service.RideService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RideControllerImpl implements RideController {

    private final RideService rideService;

    public RideControllerImpl(RideService rideService) {
        this.rideService = rideService;
    }

    @Override
    public ResponseEntity<RideResponse> requestRide(RideRequest request) {
        RideResponse ride = RideResponse.from(
                rideService.requestRide(request.riderId(), request.pickup().toLocation()));
        return ResponseEntity.created(URI.create(PATH + "/" + ride.rideId())).body(ride);
    }

    @Override
    public RideResponse completeRide(String rideId, CompleteRideRequest request) {
        return RideResponse.from(rideService.completeRide(rideId, request.riderId()));
    }

    @Override
    public RideResponse getRide(String rideId) {
        return RideResponse.from(rideService.getRide(rideId));
    }
}
