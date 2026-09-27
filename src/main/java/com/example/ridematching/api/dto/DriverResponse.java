package com.example.ridematching.api.dto;

import com.example.ridematching.domain.DriverState;

public record DriverResponse(String driverId, LocationDto location, String status) {

    public static DriverResponse from(String driverId, DriverState state) {
        return new DriverResponse(driverId, LocationDto.from(state.location()), state.status().name());
    }
}
