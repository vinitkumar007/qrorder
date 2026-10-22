package com.hotel.qrorder.repo;

import com.hotel.qrorder.model.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepo extends JpaRepository<AppSetting, String> {}
