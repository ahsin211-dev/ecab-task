package com.example.ridematching.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CompleteRideRequest(@NotBlank String riderId) {
}
