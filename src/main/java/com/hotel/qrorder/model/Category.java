package com.hotel.qrorder.model;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "category")
@Getter @Setter @NoArgsConstructor
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
