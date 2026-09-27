package com.example.ridematching.matching;

import com.example.ridematching.domain.Location;

public interface DistanceCalculator {

    double distance(Location from, Location to);
}
