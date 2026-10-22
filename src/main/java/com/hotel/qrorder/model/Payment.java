package com.hotel.qrorder.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Entity @Table(name = "payment")
@Getter @Setter @NoArgsConstructor
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id")
    private DiningTable table;
    @Column(nullable = false) private BigDecimal subtotal;
    @Column(name = "gst_percent", nullable = false) private BigDecimal gstPercent;
    @Column(nullable = false) private BigDecimal tax;
    @Column(name = "service_percent", nullable = false) private BigDecimal servicePercent;
    @Column(nullable = false) private BigDecimal service;
    @Column(nullable = false) private BigDecimal total;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayMethod method;
    @Column(nullable = false) private String cashier;
    @Column(name = "paid_at", nullable = false)
    private Instant paidAt = Instant.now();
}
