package com.hotel.qrorder.web;

import com.hotel.qrorder.dto.Dto;
import com.hotel.qrorder.model.StaffUser;
import com.hotel.qrorder.repo.StaffUserRepo;
import com.hotel.qrorder.service.ReportService;
import com.hotel.qrorder.service.SettingsService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** ADMIN: staff, settings aur reports. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOpsController {
    private static final Set<String> ROLES = Set.of("ADMIN", "KITCHEN", "WAITER");

    private final StaffUserRepo staff;
    private final PasswordEncoder encoder;
    private final SettingsService settings;
    private final ReportService reports;

    private static ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }

    private StaffUser find(Long id) {
        return staff.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User nahi mila"));
    }

    // ---------------- staff ----------------
    @GetMapping("/staff")
    public List<Dto.StaffView> listStaff() {
        return staff.findAllByOrderByUsernameAsc().stream()
                .map(u -> new Dto.StaffView(u.getId(), u.getUsername(), u.getRole())).toList();
    }

    @PostMapping("/staff")
    public Dto.StaffView addStaff(@Valid @RequestBody Dto.StaffRequest req) {
        String role = req.role().trim().toUpperCase();
        if (!ROLES.contains(role)) throw bad("Role galat hai (ADMIN, KITCHEN ya WAITER)");
        if (staff.findByUsername(req.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ye username pehle se hai");
        }
        StaffUser u = new StaffUser();
        u.setUsername(req.username());
        u.setRole(role);
        u.setPasswordHash(encoder.encode(req.password()));
        u = staff.save(u);
        return new Dto.StaffView(u.getId(), u.getUsername(), u.getRole());
    }

    @PostMapping("/staff/{id}/password")
    public Map<String, String> resetPassword(@PathVariable Long id, @Valid @RequestBody Dto.PasswordRequest req) {
        StaffUser u = find(id);
        u.setPasswordHash(encoder.encode(req.password()));
        staff.save(u);
        return Map.of("message", "Password badal gaya");
    }

    @DeleteMapping("/staff/{id}")
    public void deleteStaff(@PathVariable Long id, Authentication auth) {
        StaffUser u = find(id);
        if (u.getUsername().equals(auth.getName())) throw bad("Apne aap ko delete nahi kar sakte");
        if (u.getRole().equals("ADMIN") && staff.countByRole("ADMIN") <= 1) throw bad("Aakhri admin delete nahi ho sakta");
        staff.delete(u);
    }

    // ---------------- settings ----------------
    @GetMapping("/settings")
    public Dto.SettingsView getSettings() {
        return settings.view();
    }

    @PutMapping("/settings")
    public Dto.SettingsView putSettings(@Valid @RequestBody Dto.SettingsRequest req) {
        return settings.update(req);
    }

    // ---------------- reports ----------------
    @GetMapping("/reports")
    public Map<String, Object> report(@RequestParam(defaultValue = "7") int days) {
        return reports.summary(days);
    }
}
