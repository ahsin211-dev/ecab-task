package com.example.ridematching.service;

public interface DataSeeder {

    /**
     * Loads the initial data set and returns how many records were stored.
     */
    int seed();
}
