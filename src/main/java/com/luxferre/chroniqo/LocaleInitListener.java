package com.luxferre.chroniqo;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Vaadin service-init listener that synchronizes the UI locale with the
 * browser's preferred locale on every new UI initialization.
 *
 * <p>The locale is resolved in priority order: browser locale, session
 * locale, UI locale, falling back to {@link Locale#UK} when none
 * of those are available. The resolved locale is applied to both the
 * {@link VaadinSession} and the {@link UI}
 * so that date/time formatting is consistent throughout the session.
 *
 * @author Luxferre86
 * @since 15.02.2026
 */
@Slf4j
@Component
public class LocaleInitListener implements VaadinServiceInitListener {

    /**
     * Called once per Vaadin service initialisation. Registers a UI-init
     * listener that applies the resolved locale to every new UI instance.
     *
     * @param event the service-init event provided by Vaadin
     */
    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            UI ui = uiEvent.getUI();
            VaadinSession session = ui.getSession();
            WebBrowser browser = session.getBrowser();

            Locale userLocale = Arrays.stream(new Locale[]{browser.getLocale(), session.getLocale(), ui.getLocale()}).filter(Objects::nonNull).findFirst().orElse(Locale.UK);
            session.setLocale(userLocale);
            ui.setLocale(userLocale);
            log.info("User locale: {}", userLocale);
        });
    }
}