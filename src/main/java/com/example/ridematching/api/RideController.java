package com.example.ridematching.api;

import com.example.ridematching.api.dto.CompleteRideRequest;
import com.example.ridematching.api.dto.ErrorResponse;
import com.example.ridematching.api.dto.RideRequest;
import com.example.ridematching.api.dto.RideResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Rides", description = "Book, complete and look up rides")
@RequestMapping(RideController.PATH)
public interface RideController {

    String PATH = "/api/v1/rides";

    @Operation(summary = "Request a ride", description = "Assigns the nearest available driver to the rider.")
    @ApiResponse(responseCode = "201", description = "Ride started; Location header points to it")
    @ApiResponse(responseCode = "409", description = "NO_DRIVER_AVAILABLE or RIDER_HAS_ACTIVE_RIDE",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    ResponseEntity<RideResponse> requestRide(@Valid @RequestBody RideRequest request);

    @Operation(summary = "Complete a ride", description = "Only the rider who booked it can complete it; frees the driver.")
    @ApiResponse(responseCode = "403", description = "RIDE_ACCESS_DENIED: another rider's ride",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{rideId}/complete")
    RideResponse completeRide(@Parameter(description = "Ride id") @PathVariable String rideId,
                              @Valid @RequestBody CompleteRideRequest request);

    @Operation(summary = "Get a ride")
    @ApiResponse(responseCode = "200", description = "The ride")
    @ApiResponse(responseCode = "404", description = "Ride not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{rideId}")
    RideResponse getRide(@Parameter(description = "Ride id") @PathVariable String rideId);
}
