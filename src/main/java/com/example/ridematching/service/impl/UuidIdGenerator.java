package com.example.ridematching.service.impl;

import com.example.ridematching.service.IdGenerator;

import java.util.UUID;

public class UuidIdGenerator implements IdGenerator {

    @Override
    public String nextId() {
        return UUID.randomUUID().toString();
    }
}
