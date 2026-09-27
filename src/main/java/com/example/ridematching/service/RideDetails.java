package com.example.ridematching.service;

import com.example.ridematching.domain.DriverState;
import com.example.ridematching.domain.Ride;

/**
 * A ride together with a snapshot of its driver, which is what a rider gets back.
 */
public record RideDetails(Ride ride, DriverState driver) {
}
