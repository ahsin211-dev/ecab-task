package com.example.ridematching.service.impl;

import com.example.ridematching.domain.DriverStatus;
import com.example.ridematching.support.InMemoryRideMatching;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Uses src/test/resources/data/test-drivers.json: "near" and "far" available, "offline" not. */
class JsonDriverDataSeederTest {

    private final InMemoryRideMatching app = new InMemoryRideMatching();

    /** Seeds from the test file and checks 3 drivers are stored with their available/offline status. */
    @Test
    void storesEveryDriverWithItsAvailability() {
        int seeded = seederFor("data/test-drivers.json").seed();

        assertThat(seeded).isEqualTo(3);
        assertThat(app.statusOf("near")).isEqualTo(DriverStatus.AVAILABLE);
        assertThat(app.statusOf("offline")).isEqualTo(DriverStatus.OFFLINE);
    }

    /** Seeds from a file that does not exist and expects UncheckedIOException naming the file. */
    @Test
    void missingFileFailsWithItsName() {
        assertThatThrownBy(() -> seederFor("data/missing.json").seed())
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("missing.json");
    }

    private JsonDriverDataSeeder seederFor(String path) {
        return new JsonDriverDataSeeder(app.drivers(), JsonMapper.builder().build(), new ClassPathResource(path));
    }
}
