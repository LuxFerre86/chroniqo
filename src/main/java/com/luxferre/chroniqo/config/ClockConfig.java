package com.luxferre.chroniqo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides the application-wide {@link Clock} bean.
 *
 * <p>Centralising the clock as a Spring bean allows services to use
 * {@code LocalDate.now(clock)} instead of {@code LocalDate.now()}, making
 * time-dependent logic deterministic and easy to test with a fixed clock.
 *
 * @author Luxferre86
 * @since 06.04.2026
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
