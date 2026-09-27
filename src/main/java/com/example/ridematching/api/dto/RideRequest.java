package com.example.ridematching.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RideRequest(@NotBlank String riderId, @NotNull @Valid LocationDto pickup) {
}
