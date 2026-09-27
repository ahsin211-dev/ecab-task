package com.example.ridematching.api.impl;

import com.example.ridematching.api.DriverController;
import com.example.ridematching.api.dto.DriverResponse;
import com.example.ridematching.api.dto.LocationDto;
import com.example.ridematching.api.dto.NearbyDriverResponse;
import com.example.ridematching.api.dto.UpdateDriverRequest;
import com.example.ridematching.domain.DriverState;
import com.example.ridematching.domain.Location;
import com.example.ridematching.service.DriverService;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DriverControllerImpl implements DriverController {

    private final DriverService driverService;

    public DriverControllerImpl(DriverService driverService) {
        this.driverService = driverService;
    }

    @Override
    public List<DriverResponse> findAll() {
        return driverService.findAll().stream()
                .map(driver -> DriverResponse.from(driver.driverId(), driver.state()))
                .toList();
    }

    @Override
    public DriverResponse updateDriver(String driverId, UpdateDriverRequest request) {
        DriverState state = driverService.updateDriver(driverId, request.toLocation(), request.available());
        return DriverResponse.from(driverId, state);
    }

    @Override
    public DriverResponse updateLocation(String driverId, LocationDto location) {
        DriverState state = driverService.updateLocation(driverId, location.toLocation());
        return DriverResponse.from(driverId, state);
    }

    @Override
    public List<NearbyDriverResponse> findNearest(double x, double y, int limit) {
        return driverService.findNearestAvailable(new Location(x, y), limit).stream()
                .map(NearbyDriverResponse::from)
                .toList();
    }
}
