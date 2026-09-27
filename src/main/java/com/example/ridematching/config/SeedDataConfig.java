package com.example.ridematching.config;

import com.example.ridematching.service.DataSeeder;
import com.example.ridematching.service.DriverService;
import com.example.ridematching.service.impl.JsonDriverDataSeeder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class SeedDataConfig {

    private static final Logger log = LoggerFactory.getLogger(SeedDataConfig.class);

    @Bean
    public DataSeeder driverDataSeeder(DriverService driverService,
                                       JsonMapper jsonMapper,
                                       @Value("${app.seed.drivers-file:classpath:data/drivers.json}") Resource seedFile) {
        return new JsonDriverDataSeeder(driverService, jsonMapper, seedFile);
    }

    /**
     * @param driverDataSeeder matches    public DataSeeder driverDataSeeder(...)
     */
    @Bean
    public ApplicationRunner seedOnStartup(DataSeeder driverDataSeeder) {
        return args -> log.info("Seeded {} drivers", driverDataSeeder.seed());
    }
}
