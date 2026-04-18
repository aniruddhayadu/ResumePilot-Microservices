package com.resumepilot.auth.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String fullName;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;
    private String phone;
    private String role; // FREE / PREMIUM / ADMIN
    private String subscriptionPlan;
    private boolean isActive = true;
    private LocalDateTime createdAt;
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}