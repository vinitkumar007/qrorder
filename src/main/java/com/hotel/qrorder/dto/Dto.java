package com.hotel.qrorder.dto;

import com.hotel.qrorder.model.MenuItem;
import com.hotel.qrorder.model.WaiterCall;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Dto {
    private Dto() {}

    // ----- requests -----
    public record OrderLine(@NotNull Long menuItemId, @Min(1) @Max(20) int quantity) {}
    public record OrderRequest(@NotEmpty @Size(max = 30) List<@Valid OrderLine> items, @Size(max = 200) String note) {}
    public record WaiterRequest(@NotBlank String reason) {}
    public record StatusRequest(@NotBlank String status) {}
    public record PayRequest(@NotBlank String method) {}
    public record NewTableRequest(@NotNull @Min(1) @Max(999) Integer tableNumber) {}
    public record NewCategoryRequest(@NotBlank @Size(max = 80) String name) {}
    public record NewMenuItemRequest(
            @NotNull Long categoryId,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description,
            @NotNull @DecimalMin("0.0") @DecimalMax("100000") BigDecimal price,
            @Size(max = 500) String imageUrl,
            Boolean veg) {}
    public record StaffRequest(
            @NotBlank @Size(min = 3, max = 40) @Pattern(regexp = "[A-Za-z0-9_.-]+", message = "sirf letters, numbers, _ . - allowed") String username,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank String role) {}
    public record PasswordRequest(@NotBlank @Size(min = 8, max = 72) String password) {}
    public record PasswordChangeRequest(@NotBlank String current, @NotBlank @Size(min = 8, max = 72) String newPassword) {}
    public record SettingsRequest(
            @NotBlank @Size(max = 60) String restaurantName,
            @NotNull @DecimalMin("0") @DecimalMax("28") BigDecimal gstPercent,
            @NotNull @DecimalMin("0") @DecimalMax("20") BigDecimal servicePercent) {}

    // ----- responses -----
    public record MenuItemView(Long id, String name, String description, BigDecimal price, String imageUrl,
                               Boolean veg, boolean available, String category, Long categoryId) {}
    public record CategoryMenu(String name, List<MenuItemView> items) {}
    public record MenuResponse(String restaurantName, int tableNumber, List<CategoryMenu> categories) {}
    public record CategoryView(Long id, String name) {}
    public record LineView(String name, int quantity) {}
    public record OrderView(Long id, int tableNumber, String status, String note, BigDecimal total,
                            Instant createdAt, List<LineView> items) {}
    public record TableView(Long id, int tableNumber, boolean active, String url) {}
    public record WaiterCallView(Long id, int tableNumber, String reason, String reasonText, Instant createdAt) {}
    public record StaffView(Long id, String username, String role) {}
    public record SettingsView(String restaurantName, BigDecimal gstPercent, BigDecimal servicePercent) {}

    public record BillLine(String name, int quantity, BigDecimal unitPrice, BigDecimal amount) {}
    public record BillOrder(Long id, String status, Instant createdAt, String note, List<BillLine> lines) {}
    public record BillView(Long tableId, int tableNumber, List<BillOrder> orders, BigDecimal subtotal,
                           BigDecimal gstPercent, BigDecimal tax, BigDecimal servicePercent, BigDecimal service,
                           BigDecimal total, boolean allServed) {}
    public record Receipt(Long id, String restaurantName, Instant paidAt, String method, String cashier, BillView bill) {}

    public static MenuItemView menuView(MenuItem m) {
        return new MenuItemView(m.getId(), m.getName(), m.getDescription(), m.getPrice(), m.getImageUrl(),
                m.getVeg(), m.isAvailable(), m.getCategory().getName(), m.getCategory().getId());
    }

    public static WaiterCallView callView(WaiterCall c) {
        return new WaiterCallView(c.getId(), c.getTable().getTableNumber(), c.getReason().name(),
                c.getReason().getText(), c.getCreatedAt());
    }
}
