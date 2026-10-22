package com.hotel.qrorder.service;

import com.hotel.qrorder.dto.Dto;
import com.hotel.qrorder.model.*;
import com.hotel.qrorder.repo.MenuItemRepo;
import com.hotel.qrorder.repo.OrderRepo;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final MenuItemRepo menuRepo;
    private final OrderRepo orderRepo;

    /** Price hamesha DB se aata hai, customer se kabhi nahi. */
    @Transactional
    public OrderEntity place(DiningTable table, Dto.OrderRequest req) {
        Map<Long, Integer> qty = new LinkedHashMap<>();
        for (Dto.OrderLine l : req.items()) qty.merge(l.menuItemId(), l.quantity(), Integer::sum);

        List<MenuItem> found = menuRepo.findAllById(qty.keySet());
        if (found.size() != qty.size()) throw bad("Menu ka koi item mila nahi");

        OrderEntity order = new OrderEntity();
        order.setTable(table);
        order.setNote(req.note() == null || req.note().isBlank() ? null : req.note().trim());

        BigDecimal total = BigDecimal.ZERO;
        for (MenuItem m : found) {
            if (!m.isAvailable()) throw bad(m.getName() + " abhi available nahi hai");
            int q = qty.get(m.getId());
            if (q > 20) throw bad("Ek item ki quantity 20 se zyada nahi ho sakti");
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setMenuItem(m);
            oi.setQuantity(q);
            oi.setUnitPrice(m.getPrice());
            order.getItems().add(oi);
            total = total.add(m.getPrice().multiply(BigDecimal.valueOf(q)));
        }
        order.setTotal(total);
        return orderRepo.save(order);
    }

    public static Dto.OrderView view(OrderEntity o) {
        List<Dto.LineView> lines = o.getItems().stream()
                .map(i -> new Dto.LineView(i.getMenuItem().getName(), i.getQuantity()))
                .toList();
        return new Dto.OrderView(o.getId(), o.getTable().getTableNumber(), o.getStatus().name(),
                o.getNote(), o.getTotal(), o.getCreatedAt(), lines);
    }

    @Transactional(readOnly = true)
    public List<Dto.OrderView> active() {
        return orderRepo.findActive(List.of(OrderStatus.NEW, OrderStatus.PREPARING, OrderStatus.READY))
                .stream().map(OrderService::view).toList();
    }

    @Transactional
    public Dto.OrderView setStatus(Long id, OrderStatus status) {
        OrderEntity o = orderRepo.findWithItems(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order nahi mila"));
        if (o.getPaymentId() != null || o.getStatus() == OrderStatus.SERVED || o.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Is order ka status ab badal nahi sakte");
        }
        o.setStatus(status);
        return view(o);
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
