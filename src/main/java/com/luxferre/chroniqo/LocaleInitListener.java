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

@Slf4j
@Component
public class LocaleInitListener implements VaadinServiceInitListener {

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
