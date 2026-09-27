package com.example.ridematching.repository.impl;

import com.example.ridematching.domain.Ride;
import com.example.ridematching.repository.RideRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryRideRepository implements RideRepository {

    private final Map<String, Ride> rides = new ConcurrentHashMap<>();

    @Override
    public Ride save(Ride ride) {
        rides.put(ride.id(), ride);
        return ride;
    }

    @Override
    public Optional<Ride> findById(String id) {
        return Optional.ofNullable(rides.get(id));
    }
}
