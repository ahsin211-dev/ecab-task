package com.example.ridematching.service;

import com.example.ridematching.domain.DriverState;

/** One immutable snapshot of a driver's location and status. */
public record DriverDetails(String driverId, DriverState state) {
}
