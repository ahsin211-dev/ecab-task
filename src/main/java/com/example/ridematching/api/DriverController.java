package com.example.ridematching.api;

import com.example.ridematching.api.dto.DriverResponse;
import com.example.ridematching.api.dto.LocationDto;
import com.example.ridematching.api.dto.NearbyDriverResponse;
import com.example.ridematching.api.dto.UpdateDriverRequest;
import com.example.ridematching.service.DriverService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping(DriverController.PATH)
public interface DriverController {

    String PATH = "/api/v1/drivers";

    @GetMapping
    List<DriverResponse> findAll();

    @PutMapping("/{driverId}")
    DriverResponse updateDriver(@PathVariable String driverId,
                                @Valid @RequestBody UpdateDriverRequest request);

    @PutMapping("/{driverId}/location")
    DriverResponse updateLocation(@PathVariable String driverId,
                                  @Valid @RequestBody LocationDto location);

    @GetMapping("/nearest")
    List<NearbyDriverResponse> findNearest(
            @RequestParam double x,
            @RequestParam double y,
            @RequestParam(defaultValue = "5")
            @Min(DriverService.MIN_NEAREST_LIMIT) @Max(DriverService.MAX_NEAREST_LIMIT) int limit);
}
