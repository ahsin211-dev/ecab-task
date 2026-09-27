package com.example.ridematching.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocationTest {

    @Test
    void rejectsNotANumber() {
        assertThatThrownBy(() -> new Location(Double.NaN, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInfinity() {
        assertThatThrownBy(() -> new Location(0, Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
