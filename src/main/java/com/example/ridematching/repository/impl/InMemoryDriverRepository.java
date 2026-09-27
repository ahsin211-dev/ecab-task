package com.example.ridematching.repository.impl;

import com.example.ridematching.domain.Driver;
import com.example.ridematching.repository.DriverRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Repository
public class InMemoryDriverRepository implements DriverRepository {

    private final Map<String, Driver> drivers = new ConcurrentHashMap<>();

    @Override
    public Driver save(Driver driver) {
        drivers.put(driver.id(), driver);
        return driver;
    }

    @Override
    public Optional<Driver> findById(String id) {
        return Optional.ofNullable(drivers.get(id));
    }

    @Override
    public Collection<Driver> findAvailable() {
        return drivers.values().stream()
                .filter(driver -> driver.currentState().isAvailable())
                .toList();
    }

    @Override
    public Collection<Driver> findAll() {
        return List.copyOf(drivers.values());
    }

    @Override
    public Driver computeIfAbsent(String id, Function<String, Driver> factory) {
        return drivers.computeIfAbsent(id, factory);
    }
}
