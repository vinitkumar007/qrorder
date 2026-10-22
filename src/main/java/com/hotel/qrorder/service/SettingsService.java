package com.hotel.qrorder.service;

import com.hotel.qrorder.dto.Dto;
import com.hotel.qrorder.model.AppSetting;
import com.hotel.qrorder.repo.AppSettingRepo;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingsService {
    private final AppSettingRepo repo;

    public String get(String key, String def) {
        return repo.findById(key).map(AppSetting::getValue).orElse(def);
    }

    public BigDecimal decimal(String key, String def) {
        try {
            return new BigDecimal(get(key, def));
        } catch (NumberFormatException e) {
            return new BigDecimal(def);
        }
    }

    @Transactional
    public void set(String key, String value) {
        AppSetting s = repo.findById(key).orElseGet(() -> {
            AppSetting n = new AppSetting();
            n.setKey(key);
            return n;
        });
        s.setValue(value);
        repo.save(s);
    }

    public Dto.SettingsView view() {
        return new Dto.SettingsView(get("restaurant_name", "Hotel Rasoi"),
                decimal("gst_percent", "5"), decimal("service_percent", "0"));
    }

    @Transactional
    public Dto.SettingsView update(Dto.SettingsRequest r) {
        set("restaurant_name", r.restaurantName().trim());
        set("gst_percent", r.gstPercent().toPlainString());
        set("service_percent", r.servicePercent().toPlainString());
        return view();
    }
}
