package com.luxferre.chroniqo.frontend;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;

@Layout
public class AppLayoutBasic extends AppLayout {

    public AppLayoutBasic() {
        addClassName("chroniqo-app-layout");

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

        // Logo/Icon (optional)
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

        // SideNav
        SideNav nav = getSideNav();
        nav.getStyle()
                .set("margin", "var(--lumo-space-m)");

        Scroller scroller = new Scroller(nav);
        scroller.getStyle()
                .set("background", "transparent");

        // Drawer Footer (optional - version info)
        Span version = new Span("v1.0.0");
        version.getStyle()
                .set("font-size", "11px")
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("text-align", "center")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-top", "auto");

        addToDrawer(scroller, version);
        addToNavbar(toggle, titleLayout);

        // Navbar Styling
        getElement().executeJs(
                "const navbar = this.shadowRoot.querySelector('[part=\"navbar\"]');" +
                        "if (navbar) {" +
                        "  navbar.style.background = 'linear-gradient(180deg, hsl(220, 20%, 13%) 0%, hsl(220, 20%, 11%) 100%)';" +
                        "  navbar.style.borderBottom = '1px solid hsla(32, 40%, 50%, 0.15)';" +
                        "  navbar.style.boxShadow = '0 2px 8px rgba(0, 0, 0, 0.5)';" +
                        "  navbar.style.backdropFilter = 'blur(8px)';" +
                        "}"
        );

        // Drawer Styling
        getElement().executeJs(
                "const drawer = this.shadowRoot.querySelector('[part=\"drawer\"]');" +
                        "if (drawer) {" +
                        "  drawer.style.background = 'linear-gradient(180deg, hsl(220, 20%, 13%) 0%, hsl(220, 20%, 10%) 100%)';" +
                        "  drawer.style.borderRight = '1px solid hsla(32, 40%, 50%, 0.15)';" +
                        "  drawer.style.boxShadow = '4px 0 16px rgba(0, 0, 0, 0.5)';" +
                        "}"
        );
    }

    private SideNav getSideNav() {
        // Nav Items
        SideNavItem monthView = new SideNavItem("Monthly View", MonthView.class);

        // Custom styling for nav items
        styleNavItem(monthView);

        SideNav sideNav = new SideNav();
        sideNav.addItem(monthView);
        sideNav.setCollapsible(false);

        return sideNav;
    }

    private void styleNavItem(SideNavItem item) {
        item.getStyle()
                .set("border-radius", "8px")
                .set("margin-bottom", "0.25rem")
                .set("transition", "all 0.2s ease");

        // Hover & Active state wird über CSS gesteuert (siehe app-layout.css)
    }
}