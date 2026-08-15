package com.luxferre.chroniqo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Chroniqo time-tracking application.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
@SpringBootApplication
public class ChroniqoApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments passed to the Spring Boot launcher
     */
    public static void main(String[] args) {
        SpringApplication.run(ChroniqoApplication.class, args);
    }
}
