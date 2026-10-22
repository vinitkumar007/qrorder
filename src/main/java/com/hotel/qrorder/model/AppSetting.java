package com.hotel.qrorder.model;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "app_setting")
@Getter @Setter @NoArgsConstructor
public class AppSetting {
    @Id
    @Column(name = "setting_key", length = 60)
    private String key;
    @Column(name = "setting_value", nullable = false)
    private String value;
}
