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

/**
 * Typed configuration properties for the remember-me cookie, bound to the
 * {@code spring.security.remember-me.*} namespace in {@code application.yaml}.
 *
 * <p>{@code key} is mandatory and must not be blank — it is the HMAC secret
 * used to sign remember-me tokens. {@code cookieDomain} is optional; when
 * absent Spring Security scopes the cookie to the request domain automatically.
 * Current values are logged at startup for easier diagnostics.
 *
 * @author Luxferre86
 * @since 23.02.2026
 */
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
    /**
     * Optional. When blank or not set, Spring Security applies the cookie to the
     * domain of the current request. Set explicitly in production to scope the
     * remember-me cookie to your domain (e.g. {@code example.com}).
     */
    private String cookieDomain;
    private Duration validity;

    /**
     * Logs the resolved property values at startup for diagnostics.
     * The HMAC key is never logged in full — only whether it is set.
     */
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