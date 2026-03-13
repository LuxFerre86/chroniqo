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
