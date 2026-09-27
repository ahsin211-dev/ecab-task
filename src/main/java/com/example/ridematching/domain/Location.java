package com.example.ridematching.domain;

public record Location(double x, double y) {

    public Location {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Coordinates must be finite numbers, got (" + x + ", " + y + ")");
        }
    }
}
