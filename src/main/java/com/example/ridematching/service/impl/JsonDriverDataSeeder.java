package com.example.ridematching.service.impl;

import com.example.ridematching.domain.Location;
import com.example.ridematching.service.DataSeeder;
import com.example.ridematching.service.DriverService;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;


public class JsonDriverDataSeeder implements DataSeeder {

    private final DriverService driverService;
    private final JsonMapper jsonMapper;
    private final Resource seedFile;
    private record SeedDriver(String id, double x, double y, boolean available) {
    }
    public JsonDriverDataSeeder(DriverService driverService, JsonMapper jsonMapper, Resource seedFile) {
        this.driverService = driverService;
        this.jsonMapper = jsonMapper;
        this.seedFile = seedFile;
    }

    @Override
    public int seed() {
        List<SeedDriver> drivers = readSeedFile();
        // Go through the service so seeded drivers obey the same rules as drivers registered over HTTP.
        drivers.forEach(driver ->
                driverService.updateDriver(driver.id(), new Location(driver.x(), driver.y()), driver.available()));
        return drivers.size();
    }

    // read data from file
    private List<SeedDriver> readSeedFile() {
        try (InputStream in = seedFile.getInputStream()) {
            return List.of(jsonMapper.readValue(in, SeedDriver[].class));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read seed file " + seedFile.getDescription(), e);
        }
    }
}
