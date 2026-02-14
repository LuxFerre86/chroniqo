package com.luxferre.chroniqo.frontend;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;

@Layout
public class AppLayoutBasic extends AppLayout {

    public AppLayoutBasic() {
        DrawerToggle toggle = new DrawerToggle();

        H1 title = new H1("ChroniQo");
        title.getStyle().set("font-size", "1.125rem").set("margin", "0");

        SideNav nav = getSideNav();
        nav.getStyle().set("margin", "var(--vaadin-gap-s)");

        Scroller scroller = new Scroller(nav);

        addToDrawer(scroller);
        addToNavbar(toggle, title);
    }

    private SideNav getSideNav() {
        SideNavItem month = new SideNavItem("Monthly View", MonthView.class);

        SideNav sideNav = new SideNav();
        sideNav.addItem(month);

        return sideNav;
    }
}
