package com.hotel.qrorder.model;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "staff_user")
@Getter @Setter @NoArgsConstructor
public class StaffUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private String role;
}
