package com.hotel.qrorder.web;

import com.hotel.qrorder.dto.Dto;
import com.hotel.qrorder.model.StaffUser;
import com.hotel.qrorder.repo.StaffUserRepo;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Logged-in staff apna password khud badal sakta hai. */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    private final StaffUserRepo staff;
    private final PasswordEncoder encoder;

    @PostMapping("/password")
    public Map<String, String> change(@Valid @RequestBody Dto.PasswordChangeRequest req, Authentication auth) {
        StaffUser u = staff.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User nahi mila"));
        if (!encoder.matches(req.current(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Purana password galat hai");
        }
        u.setPasswordHash(encoder.encode(req.newPassword()));
        staff.save(u);
        return Map.of("message", "Password badal gaya");
    }
}
