package com.example.ridematching.matching;

import com.example.ridematching.domain.Driver;
import com.example.ridematching.domain.Location;

import java.util.Collection;
import java.util.List;

public interface DriverMatchingStrategy {

    /**
     * Orders the available drivers from best to worst match for the given origin.
     * Drivers that are not available at the moment of ranking are left out.
     */
    List<RankedDriver> rank(Location origin, Collection<Driver> drivers, int limit);
}
