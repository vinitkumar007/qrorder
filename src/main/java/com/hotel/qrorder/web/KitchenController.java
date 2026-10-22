package com.hotel.qrorder.web;

import com.hotel.qrorder.dto.Dto;
import com.hotel.qrorder.model.CallStatus;
import com.hotel.qrorder.model.OrderStatus;
import com.hotel.qrorder.model.WaiterCall;
import com.hotel.qrorder.repo.WaiterCallRepo;
import com.hotel.qrorder.service.LiveEventService;
import com.hotel.qrorder.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** KITCHEN, WAITER aur ADMIN (SecurityConfig me locked). Waiter sirf "SERVED" mark kar sakta hai. */
@RestController
@RequestMapping("/api/kitchen")
@RequiredArgsConstructor
public class KitchenController {
    private final OrderService orders;
    private final WaiterCallRepo waiterRepo;
    private final LiveEventService live;

    @GetMapping("/orders")
    public List<Dto.OrderView> activeOrders() {
        return orders.active();
    }

    @PatchMapping("/orders/{id}/status")
    public Dto.OrderView setStatus(@PathVariable Long id, @Valid @RequestBody Dto.StatusRequest req, Authentication auth) {
        OrderStatus status;
        try {
            status = OrderStatus.valueOf(req.status().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status galat hai");
        }
        boolean chefOrAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_KITCHEN"));
        if (!chefOrAdmin && status != OrderStatus.SERVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Waiter sirf 'parosa gaya' mark kar sakta hai");
        }
        Dto.OrderView view = orders.setStatus(id, status);
        live.publish("ORDER_UPDATED", view);
        return view;
    }

    @GetMapping("/waiter-calls")
    public List<Dto.WaiterCallView> openCalls() {
        return waiterRepo.findByStatusWithTable(CallStatus.OPEN).stream().map(Dto::callView).toList();
    }

    @PostMapping("/waiter-calls/{id}/ack")
    public Dto.WaiterCallView ack(@PathVariable Long id) {
        WaiterCall c = waiterRepo.findWithTable(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Call nahi mili"));
        c.setStatus(CallStatus.ACKNOWLEDGED);
        waiterRepo.save(c);
        Dto.WaiterCallView v = Dto.callView(c);
        live.publish("WAITER_ACK", v);
        return v;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return live.subscribe();
    }
}
