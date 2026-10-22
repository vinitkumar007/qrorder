package com.hotel.qrorder.config;

import com.hotel.qrorder.model.StaffUser;
import com.hotel.qrorder.repo.StaffUserRepo;
import com.hotel.qrorder.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Jo default users nahi hain (admin, kitchen, waiter) unhe pehli baar start pe banata hai. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {
    private final StaffUserRepo repo;
    private final PasswordEncoder encoder;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;
    @Value("${app.bootstrap.kitchen-password:}")
    private String kitchenPassword;
    @Value("${app.bootstrap.waiter-password:}")
    private String waiterPassword;

    @Override
    public void run(ApplicationArguments args) {
        createIfMissing("admin", "ADMIN", adminPassword);
        createIfMissing("kitchen", "KITCHEN", kitchenPassword);
        createIfMissing("waiter", "WAITER", waiterPassword);
    }

    private void createIfMissing(String username, String role, String password) {
        if (repo.findByUsername(username).isPresent()) return;
        boolean generated = password == null || password.isBlank();
        String pw = generated ? TokenUtil.randomPassword(14) : password;
        StaffUser u = new StaffUser();
        u.setUsername(username);
        u.setRole(role);
        u.setPasswordHash(encoder.encode(pw));
        repo.save(u);
        if (generated) {
            log.warn("=== LOGIN CREATED === username: {}  password: {}  (ye sirf ek baar dikhega, note kar lo)", username, pw);
        } else {
            log.warn("=== LOGIN CREATED === username: {} (password environment variable se)", username);
        }
    }
}
