package com.example.ridematching.matching.impl;

import com.example.ridematching.domain.Location;
import com.example.ridematching.matching.DistanceCalculator;
import org.springframework.stereotype.Component;

@Component
public class EuclideanDistanceCalculator implements DistanceCalculator {

    @Override
    public double distance(Location from, Location to) {
        // hypot avoids overflow/underflow that a naive sqrt(dx*dx + dy*dy) hits with extreme coordinates
        return Math.hypot(to.x() - from.x(), to.y() - from.y());
    }
}
