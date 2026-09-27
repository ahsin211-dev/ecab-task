package com.example.ridematching.matching;

import com.example.ridematching.domain.DriverState;

public record RankedDriver(String driverId, DriverState snapshot, double distance) {
}
