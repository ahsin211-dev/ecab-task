package com.example.ridematching.api.dto;

import com.example.ridematching.domain.Location;
import jakarta.validation.constraints.NotNull;

public record UpdateDriverRequest(@NotNull Double x, @NotNull Double y, @NotNull Boolean available) {

    public Location toLocation() {
        return new Location(x, y);
    }
}
