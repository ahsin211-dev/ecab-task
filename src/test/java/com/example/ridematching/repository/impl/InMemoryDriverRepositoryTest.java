package com.example.ridematching.repository.impl;

import com.example.ridematching.domain.Driver;
import com.example.ridematching.domain.Location;
import com.example.ridematching.repository.DriverRepository;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryDriverRepositoryTest {

    private final DriverRepository repository = new InMemoryDriverRepository();

    @Test
    void findAvailableExcludesDriversOnRideAndOffline() {
        Driver onRide = repository.save(Driver.register("on-ride", new Location(0, 0), true));
        onRide.tryReserve();
        repository.save(Driver.register("offline", new Location(0, 0), false));
        repository.save(Driver.register("free", new Location(0, 0), true));

        assertThat(repository.findAvailable()).extracting(Driver::id).containsExactly("free");
    }

    @Test
    void computeIfAbsentKeepsExistingDriver() {
        Driver first = repository.computeIfAbsent("d1", id -> Driver.register(id, new Location(0, 0), true));
        Driver second = repository.computeIfAbsent("d1", id -> Driver.register(id, new Location(9, 9), false));

        assertThat(second).isSameAs(first);
    }
}
