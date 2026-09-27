package com.example.ridematching.support;

import com.example.ridematching.service.IdGenerator;

import java.util.concurrent.atomic.AtomicLong;

public class SequentialIdGenerator implements IdGenerator {

    private final AtomicLong counter = new AtomicLong();

    @Override
    public String nextId() {
        return "ride-" + counter.incrementAndGet();
    }
}
