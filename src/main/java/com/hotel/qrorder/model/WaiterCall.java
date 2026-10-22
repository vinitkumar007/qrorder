package com.hotel.qrorder.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity @Table(name = "waiter_call")
@Getter @Setter @NoArgsConstructor
public class WaiterCall {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id")
    private DiningTable table;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WaiterReason reason = WaiterReason.GENERAL;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallStatus status = CallStatus.OPEN;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
