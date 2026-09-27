package com.example.ridematching.api.dto;

import com.example.ridematching.domain.Location;
import jakarta.validation.constraints.NotNull;

public record LocationDto(@NotNull Double x, @NotNull Double y) {

    public static LocationDto from(Location location) {
        return new LocationDto(location.x(), location.y());
    }

    public Location toLocation() {
        return new Location(x, y);
    }
}
