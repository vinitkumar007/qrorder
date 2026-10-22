package com.hotel.qrorder.web;

import com.hotel.qrorder.dto.Dto;
import com.hotel.qrorder.model.PayMethod;
import com.hotel.qrorder.service.BillingService;
import com.hotel.qrorder.service.LiveEventService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** WAITER aur ADMIN: bill dekhna, payment lena, receipt. */
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {
    private final BillingService billing;
    private final LiveEventService live;

    @GetMapping("/open")
    public List<Dto.BillView> open() {
        return billing.openBills();
    }

    @GetMapping("/table/{id}")
    public Dto.BillView table(@PathVariable Long id) {
        return billing.forTable(id);
    }

    @PostMapping("/table/{id}/pay")
    public Dto.Receipt pay(@PathVariable Long id, @Valid @RequestBody Dto.PayRequest req, Authentication auth) {
        PayMethod method;
        try {
            method = PayMethod.valueOf(req.method().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment method galat hai");
        }
        Dto.Receipt r = billing.pay(id, method, auth.getName());
        live.publish("BILL_PAID", Map.of("tableId", id));
        return r;
    }

    @GetMapping("/payments/{id}")
    public Dto.Receipt receipt(@PathVariable Long id) {
        return billing.receipt(id);
    }
}
