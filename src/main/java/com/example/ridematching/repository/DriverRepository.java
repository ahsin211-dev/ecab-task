package com.example.ridematching.repository;

import com.example.ridematching.domain.Driver;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public interface DriverRepository {

    Driver save(Driver driver);

    Optional<Driver> findById(String id);

    /**
     * Returns a point-in-time copy; drivers may change status right after this returns.
     */
    Collection<Driver> findAvailable();

    Collection<Driver> findAll();

    /**
     * Atomically returns the existing driver or stores the one built by {@code factory}.
     */
    Driver computeIfAbsent(String id, Function<String, Driver> factory);
}
