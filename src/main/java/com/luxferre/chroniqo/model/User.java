package com.luxferre.chroniqo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "users")
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private int weeklyTargetHours;

    @ElementCollection
    @CollectionTable(name = "user_workdays", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "workday")
    private List<String> workingDays; // MONDAY, TUESDAY, ...

    private String federalState;
}
