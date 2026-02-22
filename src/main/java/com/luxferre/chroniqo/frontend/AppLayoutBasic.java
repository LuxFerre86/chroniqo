package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.service.AuthenticationService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;

@Layout
@AnonymousAllowed
public class AppLayoutBasic extends AppLayout {

    private final AuthenticationContext authenticationContext;

    public AppLayoutBasic(AuthenticationContext authenticationContext, AuthenticationService authenticationService) {
        this.authenticationContext = authenticationContext;

        addClassName("chroniqo-app-layout");

        // Get current user
        User currentUser = authenticationService.getCurrentUser().orElse(null);

        // Drawer Toggle
        DrawerToggle toggle = new DrawerToggle();
        toggle.getStyle()
                .set("color", "var(--lumo-body-text-color)");

        // Logo & Title Container
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);
        titleLayout.getStyle()
                .set("gap", "0.75rem")
                .set("padding-left", "0.5rem");

        // Logo
        Span logo = new Span("⏱");
        logo.getStyle()
                .set("font-size", "24px")
                .set("line-height", "1")
                .set("filter", "drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3))");

        // Title
        H1 title = new H1("ChroniQo");
        title.getStyle()
                .set("font-size", "20px")
                .set("font-weight", "700")
                .set("margin", "0")
                .set("color", "hsl(38, 95%, 65%)")
                .set("letter-spacing", "0.5px")
                .set("text-shadow", "0 2px 8px hsla(38, 92%, 50%, 0.4)");

        titleLayout.add(logo, title);

        // User Info & Logout (right side of navbar)
        HorizontalLayout userSection = new HorizontalLayout();
        userSection.setAlignItems(FlexComponent.Alignment.CENTER);
        userSection.setSpacing(true);
        userSection.getStyle()
                .set("gap", "1rem")
                .set("margin-left", "auto");

        if (currentUser != null) {
            // User Name
            Span userName = new Span(currentUser.getFullName());
            userName.getStyle()
                    .set("color", "var(--lumo-body-text-color)")
                    .set("font-weight", "500")
                    .set("font-size", "14px");

            // Logout Button
            Button logoutButton = new Button("Logout", e -> logout());
            logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            logoutButton.getStyle()
                    .set("color", "var(--lumo-error-text-color)");

            userSection.add(userName, logoutButton);
        }

        // Navbar Layout
        HorizontalLayout navbar = new HorizontalLayout(toggle, titleLayout, userSection);
        navbar.setWidthFull();
        navbar.setAlignItems(FlexComponent.Alignment.CENTER);

        // SideNav
        SideNav nav = getSideNav();
        nav.getStyle()
                .set("margin", "var(--lumo-space-m)");

        Scroller scroller = new Scroller(nav);
        scroller.getStyle()
                .set("background", "transparent");

        // Drawer Footer
        VerticalLayout drawerFooter = new VerticalLayout();
        drawerFooter.setPadding(true);
        drawerFooter.setSpacing(false);
        drawerFooter.addClassName("drawer-footer");

        if (currentUser != null) {
            Span userEmail = new Span(currentUser.getEmail());
            userEmail.getStyle()
                    .set("font-size", "12px")
                    .set("color", "var(--lumo-tertiary-text-color)")
                    .set("margin-bottom", "0.5rem");
            drawerFooter.add(userEmail);
        }

        Span version = new Span("v1.0.0");
        version.getStyle()
                .set("font-size", "11px")
                .set("color", "var(--lumo-tertiary-text-color)");

        drawerFooter.add(version);
        drawerFooter.getStyle()
                .set("border-top", "1px solid hsla(38, 30%, 50%, 0.08)")
                .set("margin-top", "auto");

        addToDrawer(scroller, drawerFooter);
        addToNavbar(navbar);

        // Styling
        applyCustomStyling();
    }

    private SideNav getSideNav() {
        // Nav Items
        SideNavItem dashboard = new SideNavItem("Dashboard", DashboardView.class);
        SideNavItem monthItem = new SideNavItem("Monthly View", MonthView.class);
        SideNavItem settings = new SideNavItem("Settings", SettingsView.class);

        SideNav sideNav = new SideNav();
        sideNav.addItem(dashboard, monthItem, settings);
        sideNav.setCollapsible(false);

        return sideNav;
    }

    private void logout() {
        authenticationContext.logout();
    }

    private void applyCustomStyling() {
        // Navbar Styling
        getElement().executeJs(
                "const navbar = this.shadowRoot.querySelector('[part=\"navbar\"]');" +
                        "if (navbar) {" +
                        "  navbar.style.background = 'linear-gradient(180deg, hsl(215, 22%, 13%) 0%, hsl(215, 22%, 11%) 100%)';" +
                        "  navbar.style.borderBottom = '1px solid hsla(38, 40%, 50%, 0.15)';" +
                        "  navbar.style.boxShadow = '0 2px 8px rgba(0, 0, 0, 0.5)';" +
                        "  navbar.style.backdropFilter = 'blur(8px)';" +
                        "}"
        );

        // Drawer Styling
        getElement().executeJs(
                "const drawer = this.shadowRoot.querySelector('[part=\"drawer\"]');" +
                        "if (drawer) {" +
                        "  drawer.style.background = 'linear-gradient(180deg, hsl(215, 22%, 13%) 0%, hsl(215, 22%, 10%) 100%)';" +
                        "  drawer.style.borderRight = '1px solid hsla(38, 40%, 50%, 0.15)';" +
                        "  drawer.style.boxShadow = '4px 0 16px rgba(0, 0, 0, 0.5)';" +
                        "}"
        );
    }
}