package com.example.ridematching.matching.impl;

import com.example.ridematching.domain.Location;
import com.example.ridematching.matching.DistanceCalculator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EuclideanDistanceCalculatorTest {

    private final DistanceCalculator calculator = new EuclideanDistanceCalculator();

    @Test
    void measuresThreeFourFiveTriangle() {
        assertThat(calculator.distance(new Location(0, 0), new Location(3, 4))).isEqualTo(5.0);
    }

    @Test
    void samePointIsZeroApart() {
        assertThat(calculator.distance(new Location(7, -2), new Location(7, -2))).isZero();
    }

    @Test
    void handlesNegativeCoordinates() {
        assertThat(calculator.distance(new Location(-1, -1), new Location(2, 3))).isEqualTo(5.0);
    }

    @Test
    void isSymmetric() {
        Location a = new Location(1.5, -2.25);
        Location b = new Location(-4, 10);

        assertThat(calculator.distance(a, b)).isCloseTo(calculator.distance(b, a), within(1e-12));
    }
}
