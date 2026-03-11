package com.luxferre.chroniqo.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "spring.security.remember-me")
@Setter
@Getter
@Slf4j
public class RememberMeProperties {

    private String key;
    private boolean useSecureCookie;
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
