package com.example.ridematching.api;

import com.example.ridematching.api.dto.DriverResponse;
import com.example.ridematching.api.dto.ErrorResponse;
import com.example.ridematching.api.dto.LocationDto;
import com.example.ridematching.api.dto.NearbyDriverResponse;
import com.example.ridematching.api.dto.UpdateDriverRequest;
import com.example.ridematching.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Drivers", description = "Driver registration, location updates and nearest-driver search")
@RequestMapping(DriverController.PATH)
public interface DriverController {

    String PATH = "/api/v1/drivers";

    @Operation(summary = "List all drivers", description = "Every driver with status, sorted by driver id.")
    @ApiResponse(responseCode = "200", description = "All drivers")
    @GetMapping
    List<DriverResponse> findAll();

    @Operation(summary = "Register or update a driver",
            description = "Creates the driver on first call; afterwards sets location and availability.")
    @ApiResponse(responseCode = "200", description = "Driver's new state")
    @ApiResponse(responseCode = "409", description = "DRIVER_ON_RIDE: availability cannot change during a ride",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{driverId}")
    DriverResponse updateDriver(@Parameter(description = "Driver id", example = "driver-1") @PathVariable String driverId,
                                @Valid @RequestBody UpdateDriverRequest request);

    @Operation(summary = "Update a driver's location",
            description = "Moves an existing driver without changing availability. Allowed during a ride.")
    @ApiResponse(responseCode = "200", description = "Driver's new state")
    @ApiResponse(responseCode = "404", description = "Driver not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{driverId}/location")
    DriverResponse updateLocation(@Parameter(description = "Driver id", example = "driver-1") @PathVariable String driverId,
                                  @Valid @RequestBody LocationDto location);

    @Operation(summary = "Find nearest available drivers", description = "Closest first, with straight-line distance.")
    @ApiResponse(responseCode = "200", description = "Nearest available drivers")
    @ApiResponse(responseCode = "400", description = "Missing or invalid x, y or limit",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/nearest")
    List<NearbyDriverResponse> findNearest(
            @Parameter(description = "Origin x", example = "0") @RequestParam double x,
            @Parameter(description = "Origin y", example = "0") @RequestParam double y,
            @Parameter(description = "Max results, 1 to 100") @RequestParam(defaultValue = "5")
            @Min(DriverService.MIN_NEAREST_LIMIT) @Max(DriverService.MAX_NEAREST_LIMIT) int limit);
}
