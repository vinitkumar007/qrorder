package com.hotel.qrorder.model;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "dining_table")
@Getter @Setter @NoArgsConstructor
public class DiningTable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "table_number", unique = true, nullable = false)
    private Integer tableNumber;
    @Column(name = "qr_token", unique = true, nullable = false, length = 64)
    private String qrToken;
    @Column(nullable = false)
    private boolean active = true;
}
