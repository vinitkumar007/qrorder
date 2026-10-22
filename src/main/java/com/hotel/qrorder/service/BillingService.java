package com.hotel.qrorder.service;

import com.hotel.qrorder.dto.Dto;
import com.hotel.qrorder.model.*;
import com.hotel.qrorder.repo.DiningTableRepo;
import com.hotel.qrorder.repo.OrderRepo;
import com.hotel.qrorder.repo.PaymentRepo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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
public class BillingService {
    private final OrderRepo orders;
    private final PaymentRepo payments;
    private final DiningTableRepo tables;
    private final SettingsService settings;

    private static BigDecimal pct(BigDecimal value, BigDecimal percent) {
        return value.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private static List<Dto.BillOrder> billOrders(List<OrderEntity> list) {
        return list.stream().map(o -> new Dto.BillOrder(o.getId(), o.getStatus().name(), o.getCreatedAt(), o.getNote(),
                o.getItems().stream().map(i -> new Dto.BillLine(i.getMenuItem().getName(), i.getQuantity(),
                        i.getUnitPrice(), i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))).toList()
        )).toList();
    }

    private Dto.BillView build(Long tableId, int tableNumber, List<OrderEntity> list) {
        BigDecimal subtotal = list.stream().map(OrderEntity::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gst = settings.decimal("gst_percent", "5");
        BigDecimal svc = settings.decimal("service_percent", "0");
        BigDecimal tax = pct(subtotal, gst);
        BigDecimal service = pct(subtotal, svc);
        boolean allServed = !list.isEmpty() && list.stream().allMatch(o -> o.getStatus() == OrderStatus.SERVED);
        return new Dto.BillView(tableId, tableNumber, billOrders(list), subtotal, gst, tax, svc, service,
                subtotal.add(tax).add(service), allServed);
    }

    @Transactional(readOnly = true)
    public Dto.BillView forTable(Long tableId) {
        DiningTable t = tables.findById(tableId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table nahi mili"));
        return build(t.getId(), t.getTableNumber(), orders.findUnpaidByTable(tableId, OrderStatus.CANCELLED));
    }

    @Transactional(readOnly = true)
    public List<Dto.BillView> openBills() {
        Map<Long, List<OrderEntity>> byTable = new LinkedHashMap<>();
        for (OrderEntity o : orders.findAllUnpaid(OrderStatus.CANCELLED)) {
            byTable.computeIfAbsent(o.getTable().getId(), k -> new ArrayList<>()).add(o);
        }
        List<Dto.BillView> out = new ArrayList<>();
        byTable.forEach((id, list) -> out.add(build(id, list.get(0).getTable().getTableNumber(), list)));
        return out;
    }

    @Transactional
    public Dto.Receipt pay(Long tableId, PayMethod method, String cashier) {
        DiningTable t = tables.findForUpdate(tableId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table nahi mili"));
        List<OrderEntity> list = orders.findUnpaidByTable(tableId, OrderStatus.CANCELLED);
        if (list.isEmpty()) throw bad("Is table ka koi bill baaki nahi hai");
        Dto.BillView bill = build(t.getId(), t.getTableNumber(), list);
        if (!bill.allServed()) throw bad("Kuch orders abhi serve nahi hue, pehle unhe serve karo");

        Payment p = new Payment();
        p.setTable(t);
        p.setSubtotal(bill.subtotal());
        p.setGstPercent(bill.gstPercent());
        p.setTax(bill.tax());
        p.setServicePercent(bill.servicePercent());
        p.setService(bill.service());
        p.setTotal(bill.total());
        p.setMethod(method);
        p.setCashier(cashier);
        p = payments.save(p);
        for (OrderEntity o : list) o.setPaymentId(p.getId());
        return new Dto.Receipt(p.getId(), settings.get("restaurant_name", "Hotel Rasoi"), p.getPaidAt(),
                method.name(), cashier, bill);
    }

    @Transactional(readOnly = true)
    public Dto.Receipt receipt(Long id) {
        Payment p = payments.findWithTable(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt nahi mili"));
        List<OrderEntity> list = orders.findByPayment(id);
        Dto.BillView bill = new Dto.BillView(p.getTable().getId(), p.getTable().getTableNumber(), billOrders(list),
                p.getSubtotal(), p.getGstPercent(), p.getTax(), p.getServicePercent(), p.getService(), p.getTotal(), true);
        return new Dto.Receipt(p.getId(), settings.get("restaurant_name", "Hotel Rasoi"), p.getPaidAt(),
                p.getMethod().name(), p.getCashier(), bill);
    }
}
