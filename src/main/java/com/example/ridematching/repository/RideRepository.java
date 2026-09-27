package com.example.ridematching.repository;

import com.example.ridematching.domain.Ride;

import java.util.Optional;

public interface RideRepository {

    Ride save(Ride ride);

    Optional<Ride> findById(String id);
}
