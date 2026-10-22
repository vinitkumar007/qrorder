package com.hotel.qrorder.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity @Table(name = "menu_item")
@Getter @Setter @NoArgsConstructor
public class MenuItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;
    @Column(nullable = false)
    private String name;
    private String description;
    @Column(nullable = false)
    private BigDecimal price;
    @Column(name = "image_url")
    private String imageUrl;
    private Boolean veg;
    @Column(nullable = false)
    private boolean available = true;
}
