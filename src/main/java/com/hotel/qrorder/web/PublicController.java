package com.hotel.qrorder.web;

import com.hotel.qrorder.dto.Dto;
import com.hotel.qrorder.model.*;
import com.hotel.qrorder.repo.*;
import com.hotel.qrorder.service.*;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Customer ke endpoints. Login nahi, lekin har call QR token se verify hoti hai. */
@RestController
@RequestMapping("/api/public/t/{token}")
@RequiredArgsConstructor
public class PublicController {
    private final DiningTableRepo tables;
    private final MenuItemRepo menuRepo;
    private final OrderRepo orderRepo;
    private final WaiterCallRepo waiterRepo;
    private final OrderService orders;
    private final BillingService billing;
    private final SettingsService settings;
    private final LiveEventService live;
    private final RateLimiter limiter;

    private DiningTable table(String token) {
        return tables.findByQrTokenAndActiveTrue(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QR code sahi nahi hai,dubara kosis kro"));
    }

    @GetMapping("/menu")
    public Dto.MenuResponse menu(@PathVariable String token) {
        DiningTable t = table(token);
        Map<String, List<Dto.MenuItemView>> grouped = new LinkedHashMap<>();
        for (MenuItem m : menuRepo.findAvailableWithCategory()) {
            grouped.computeIfAbsent(m.getCategory().getName(), k -> new ArrayList<>()).add(Dto.menuView(m));
        }
        List<Dto.CategoryMenu> cats = grouped.entrySet().stream()
                .map(e -> new Dto.CategoryMenu(e.getKey(), e.getValue())).toList();
        return new Dto.MenuResponse(settings.get("restaurant_name", "Hotel_Rasoi"), t.getTableNumber(), cats);
    }

    @PostMapping("/orders")
    public Map<String, Object> placeOrder(@PathVariable String token, @Valid @RequestBody Dto.OrderRequest req) {
        DiningTable t = table(token);
        if (!limiter.allow("order:" + t.getId(), 5, 60_000)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Thoda ruko, bahut saare orders ek saath ho gaye,jldi krne ki kosis krte h ");
        }
        OrderEntity o = orders.place(t, req);
        live.publish("NEW_ORDER", OrderService.view(o));
        return Map.of("orderId", o.getId(), "status", o.getStatus().name());
    }

    @GetMapping("/orders/{id}")
    public Map<String, String> orderStatus(@PathVariable String token, @PathVariable Long id) {
        DiningTable t = table(token);
        OrderEntity o = orderRepo.findByIdAndTable(id, t)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order nahi mila"));
        return Map.of("status", o.getStatus().name());
    }

    /** Is table ka chalta hua bill (abhi tak ke saare unpaid orders + tax). */
    @GetMapping("/bill")
    public Dto.BillView bill(@PathVariable String token) {
        return billing.forTable(table(token).getId());
    }

    @PostMapping("/call-waiter")
    public Map<String, String> callWaiter(@PathVariable String token, @Valid @RequestBody Dto.WaiterRequest req) {
        DiningTable t = table(token);
        WaiterReason reason;
        try {
            reason = WaiterReason.valueOf(req.reason().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reason galat hai");
        }
        if (!limiter.allow("waiter:" + t.getId(), 1, 60_000)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Waiter ko pehle hi bata diya gaya hai, thoda ruko");
        }
        WaiterCall c = new WaiterCall();
        c.setTable(t);
        c.setReason(reason);
        waiterRepo.save(c);
        live.publish("WAITER_CALL", Dto.callView(c));
        return Map.of("message", "Waiter ko bata diya, abhi aa rahe hain");
    }
}
