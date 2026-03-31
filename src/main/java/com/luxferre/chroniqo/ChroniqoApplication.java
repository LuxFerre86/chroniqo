package com.luxferre.chroniqo;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Chroniqo time-tracking application.
 *
 * <p>Bootstraps the Spring context, configures the Vaadin PWA shell, and
 * applies the application-wide Lumo Dark theme together with the custom
 * CSS overrides.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
@SpringBootApplication
@StyleSheet(Lumo.DARK)
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("app-layout.css")     // Then load custom theme overrides
@StyleSheet("dialog.css")     // Then load custom theme overrides
@StyleSheet("theme.css")     // Then load custom theme overrides
@PWA(
        name = "Chroniqo",
        shortName = "Chroniqo",
        description = "Time Tracking Application",
        themeColor = "#0F141A",
        backgroundColor = "#0F141A"
)
@Push
public class ChroniqoApplication implements AppShellConfigurator {


    /**
     * Application entry point.
     *
     * @param args command-line arguments passed to the Spring Boot launcher
     */
    public static void main(String[] args) {
        SpringApplication.run(ChroniqoApplication.class, args);
    }

}