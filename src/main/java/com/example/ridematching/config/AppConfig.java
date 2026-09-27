package com.example.ridematching.config;

import com.example.ridematching.service.IdGenerator;
import com.example.ridematching.service.impl.UuidIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public IdGenerator idGenerator() {
        return new UuidIdGenerator();
    }
}
