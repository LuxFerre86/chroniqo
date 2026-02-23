package com.luxferre.chroniqo.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.application")
@Getter
@Setter
@Slf4j
public class ApplicationInfoProperties {

    private String name;
    private String version;

    @PostConstruct
    public void log() {
        log.info("### {} ###", getClass().getSimpleName());
        log.info("# name: {}", name);
        log.info("# version: {}", version);
        log.info("### {} ###", getClass().getSimpleName());
    }
}
