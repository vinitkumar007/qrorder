package com.hotel.qrorder.web;

import com.hotel.qrorder.dto.Dto;
import com.hotel.qrorder.model.Category;
import com.hotel.qrorder.model.DiningTable;
import com.hotel.qrorder.model.MenuItem;
import com.hotel.qrorder.repo.CategoryRepo;
import com.hotel.qrorder.repo.DiningTableRepo;
import com.hotel.qrorder.repo.MenuItemRepo;
import com.hotel.qrorder.service.QrService;
import com.hotel.qrorder.service.UploadService;
import com.hotel.qrorder.util.TokenUtil;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** ADMIN: tables/QR, categories, menu, photo upload. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final DiningTableRepo tables;
    private final CategoryRepo categories;
    private final MenuItemRepo menu;
    private final QrService qr;
    private final UploadService uploads;

    @Value("${app.public-base-url}")
    private String baseUrl;

    private static ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }

    // ---------------- tables & QR ----------------
    private Dto.TableView view(DiningTable t) {
        return new Dto.TableView(t.getId(), t.getTableNumber(), t.isActive(), baseUrl + "/t/" + t.getQrToken());
    }

    private DiningTable find(Long id) {
        return tables.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table nahi mili"));
    }

    @GetMapping("/tables")
    public List<Dto.TableView> listTables() {
        return tables.findAllByOrderByTableNumberAsc().stream().map(this::view).toList();
    }

    @PostMapping("/tables")
    public Dto.TableView addTable(@Valid @RequestBody Dto.NewTableRequest req) {
        if (tables.existsByTableNumber(req.tableNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ye table number pehle se hai");
        }
        DiningTable t = new DiningTable();
        t.setTableNumber(req.tableNumber());
        t.setQrToken(TokenUtil.newToken());
        return view(tables.save(t));
    }

    /** QR leak ho gaya? Naya token banao, purana QR turant band. */
    @PostMapping("/tables/{id}/rotate")
    public Dto.TableView rotate(@PathVariable Long id) {
        DiningTable t = find(id);
        t.setQrToken(TokenUtil.newToken());
        return view(tables.save(t));
    }

    @PostMapping("/tables/{id}/toggle")
    public Dto.TableView toggleTable(@PathVariable Long id) {
        DiningTable t = find(id);
        t.setActive(!t.isActive());
        return view(tables.save(t));
    }

    @GetMapping(value = "/tables/{id}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrImage(@PathVariable Long id) throws Exception {
        DiningTable t = find(id);
        byte[] png = qr.png(baseUrl + "/t/" + t.getQrToken(), 360);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).body(png);
    }

    // ---------------- categories ----------------
    @GetMapping("/categories")
    public List<Dto.CategoryView> listCategories() {
        return categories.findAllByOrderBySortOrderAsc().stream()
                .map(c -> new Dto.CategoryView(c.getId(), c.getName())).toList();
    }

    @PostMapping("/categories")
    public Dto.CategoryView addCategory(@Valid @RequestBody Dto.NewCategoryRequest req) {
        int next = categories.findAll().stream().mapToInt(Category::getSortOrder).max().orElse(0) + 1;
        Category c = new Category();
        c.setName(req.name().trim());
        c.setSortOrder(next);
        c = categories.save(c);
        return new Dto.CategoryView(c.getId(), c.getName());
    }

    @DeleteMapping("/categories/{id}")
    public void deleteCategory(@PathVariable Long id) {
        try {
            categories.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Is category me dishes hain, pehle unhe hatao ya dusri category me daalo");
        }
    }

    // ---------------- menu ----------------
    private static String cleanImage(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.trim();
        boolean local = v.matches("/uploads/[A-Za-z0-9._-]+");
        if (!local && !v.startsWith("https://")) throw bad("Photo https:// link ho ya upload ki hui ho");
        return v;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    @GetMapping("/menu")
    public List<Dto.MenuItemView> listMenu() {
        return menu.findAllWithCategory().stream().map(Dto::menuView).toList();
    }

    @PostMapping("/menu")
    public Dto.MenuItemView addItem(@Valid @RequestBody Dto.NewMenuItemRequest req) {
        Category c = categories.findById(req.categoryId()).orElseThrow(() -> bad("Category nahi mili"));
        MenuItem m = new MenuItem();
        m.setCategory(c);
        m.setName(req.name().trim());
        m.setDescription(blankToNull(req.description()));
        m.setPrice(req.price());
        m.setImageUrl(cleanImage(req.imageUrl()));
        m.setVeg(req.veg());
        return Dto.menuView(menu.save(m));
    }

    @PutMapping("/menu/{id}")
    public Dto.MenuItemView updateItem(@PathVariable Long id, @Valid @RequestBody Dto.NewMenuItemRequest req) {
        MenuItem m = menu.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item nahi mila"));
        Category c = categories.findById(req.categoryId()).orElseThrow(() -> bad("Category nahi mili"));
        m.setCategory(c);
        m.setName(req.name().trim());
        m.setDescription(blankToNull(req.description()));
        m.setPrice(req.price());
        m.setImageUrl(cleanImage(req.imageUrl()));
        m.setVeg(req.veg());
        return Dto.menuView(menu.save(m));
    }

    @PostMapping("/menu/{id}/toggle")
    public Dto.MenuItemView toggleItem(@PathVariable Long id) {
        MenuItem m = menu.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item nahi mila"));
        m.setAvailable(!m.isAvailable());
        menu.save(m);
        return Dto.menuView(menu.findAllWithCategory().stream().filter(x -> x.getId().equals(id)).findFirst().orElseThrow());
    }

    @DeleteMapping("/menu/{id}")
    public void deleteItem(@PathVariable Long id) {
        try {
            menu.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Is item pe purane orders hain, delete nahi ho sakta. Isse 'khatam' mark kar do.");
        }
    }

    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return Map.of("url", uploads.save(file));
    }
}
