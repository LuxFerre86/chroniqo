package com.luxferre.chroniqo.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Serves the bundled React single-page application from
 * {@code classpath:/static/}.
 *
 * <p>Requests for files that do not exist (client-side routes such as
 * {@code /month} or {@code /verify-email}) fall back to {@code index.html}.
 * Paths below {@code /api} are never rewritten, so unknown API endpoints
 * correctly answer with {@code 404}. Fingerprinted build assets are cached
 * for a year, everything else (notably {@code index.html} and the service
 * worker) is always revalidated.
 *
 * @author Luxferre86
 * @since 14.11.2026
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    private static final String STATIC_LOCATION = "classpath:/static/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(STATIC_LOCATION + "assets/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

        registry.addResourceHandler("/**")
                .addResourceLocations(STATIC_LOCATION)
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new SpaFallbackResourceResolver());
    }

    /**
     * Resolves existing static files and falls back to {@code index.html} for
     * everything that is not an API path.
     */
    static final class SpaFallbackResourceResolver extends PathResourceResolver {

        private static final Resource INDEX = new ClassPathResource("static/index.html");

        @Override
        protected Resource getResource(@NonNull String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (requested.exists() && requested.isReadable()) {
                return checkResource(requested, location) ? requested : null;
            }
            if (resourcePath.startsWith("api/") || resourcePath.equals("api") || resourcePath.startsWith("actuator")) {
                return null;
            }
            // A missing file (robots.txt, *.map, ...) is a real 404; only extension-less paths are SPA routes.
            String lastSegment = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            if (lastSegment.contains(".")) {
                return null;
            }
            return INDEX.exists() ? INDEX : null;
        }
    }
}
