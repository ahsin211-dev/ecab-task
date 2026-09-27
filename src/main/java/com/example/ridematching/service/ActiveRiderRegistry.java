package com.example.ridematching.service;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces "one active ride per rider". Kept separate from the ride repository so the
 * check-and-claim is a single atomic set insertion rather than a search over rides.
 */
@Component
public class ActiveRiderRegistry {

    private final Set<String> ridersWithActiveRide = ConcurrentHashMap.newKeySet();

    public boolean tryRegister(String riderId) {
        return ridersWithActiveRide.add(riderId);
    }

    public void unregister(String riderId) {
        ridersWithActiveRide.remove(riderId);
    }
}
