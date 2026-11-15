package com.luxferre.chroniqo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Annual vacation entitlement of a user in whole days for one calendar year.
 *
 * @author Luxferre86
 * @since 15.11.2026
 */
@Getter
@Setter
@Entity
@Table(name = "vacation_entitlements",
        uniqueConstraints = @UniqueConstraint(name = "uk_vacation_entitlement_01", columnNames = {"user_id", "entitlement_year"}))
@NoArgsConstructor
public class VacationEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "entitlement_year", nullable = false)
    private int year;

    @Column(nullable = false)
    private int days;
}
