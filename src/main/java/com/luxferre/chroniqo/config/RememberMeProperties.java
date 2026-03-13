package com.luxferre.chroniqo.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "spring.security.remember-me")
@Setter
@Getter
@Slf4j
@Validated
public class RememberMeProperties {

    @NotBlank
    private String key;
    private boolean useSecureCookie;
    @NotBlank
    private String cookieDomain;
    private Duration validity;

    @PostConstruct
    public void log() {
        log.info("### {} ###", ClassUtils.getUserClass(getClass()).getSimpleName());
        log.info("# key:             [set={}]", key != null && !key.isBlank());
        log.info("# useSecureCookie: {}", useSecureCookie);
        log.info("# cookieDomain:    {}", cookieDomain);
        log.info("# validity:        {}", validity);
        log.info("### {} ###", ClassUtils.getUserClass(getClass()).getSimpleName());
    }
}
