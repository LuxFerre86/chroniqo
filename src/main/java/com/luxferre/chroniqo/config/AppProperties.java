package com.luxferre.chroniqo.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration properties bound to the {@code app.*} namespace in
 * {@code application.yaml}.
 *
 * <p>All required properties are validated with Bean Validation at startup;
 * a missing or blank value will prevent the application from starting.
 * Current values are logged via {@link PostConstruct} for
 * easier diagnostics.
 *
 * @author Luxferre86
 * @since 23.02.2026
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
@Slf4j
@Validated
public class AppProperties {

    @NotBlank
    private String baseUrl;
    private String name;
    private String version;
    @NestedConfigurationProperty
    @Name("registration")
    private RegistrationProperties registrationProperties;

    /**
     * Logs the resolved property values at startup for diagnostics.
     */
    @PostConstruct
    public void log() {
        log.info("### {} ###", ClassUtils.getUserClass(getClass()).getSimpleName());
        log.info("# base-url: {}", baseUrl);
        log.info("# name: {}", name);
        log.info("# registration.enabled: {}", registrationProperties.isEnabled());
        log.info("# version: {}", version);
        log.info("### {} ###", ClassUtils.getUserClass(getClass()).getSimpleName());
    }

    @Data
    public static class RegistrationProperties {
        private boolean enabled;
    }
}