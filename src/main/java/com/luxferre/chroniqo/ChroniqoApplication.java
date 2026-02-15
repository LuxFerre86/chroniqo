package com.luxferre.chroniqo;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@StyleSheet(Lumo.DARK)
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("app-layout.css")     // Then load custom theme overrides
@StyleSheet("dialog.css")     // Then load custom theme overrides
@StyleSheet("theme.css")     // Then load custom theme overrides
@PWA(
        name = "Chroniqo",
        shortName = "Chroniqo",
        description = "Time Tracking Application"
)
public class ChroniqoApplication implements AppShellConfigurator {


    public static void main(String[] args) {
        SpringApplication.run(ChroniqoApplication.class, args);
    }

}
