package com.example.ridematching.api.dto;

import com.example.ridematching.matching.RankedDriver;

public record NearbyDriverResponse(String driverId, LocationDto location, double distance) {

    public static NearbyDriverResponse from(RankedDriver rankedDriver) {
        return new NearbyDriverResponse(
                rankedDriver.driverId(),
                LocationDto.from(rankedDriver.snapshot().location()),
                rankedDriver.distance());
    }
}
