package com.example.ridematching.api;

import com.example.ridematching.api.dto.CompleteRideRequest;
import com.example.ridematching.api.dto.RideRequest;
import com.example.ridematching.api.dto.RideResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping(RideController.PATH)
public interface RideController {

    String PATH = "/api/v1/rides";

    @PostMapping
    ResponseEntity<RideResponse> requestRide(@Valid @RequestBody RideRequest request);

    @PostMapping("/{rideId}/complete")
    RideResponse completeRide(@PathVariable String rideId,
                              @Valid @RequestBody CompleteRideRequest request);

    @GetMapping("/{rideId}")
    RideResponse getRide(@PathVariable String rideId);
}
