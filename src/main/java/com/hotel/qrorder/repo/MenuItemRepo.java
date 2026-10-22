package com.hotel.qrorder.repo;

import com.hotel.qrorder.model.MenuItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MenuItemRepo extends JpaRepository<MenuItem, Long> {
    @Query("select m from MenuItem m join fetch m.category c where m.available = true order by c.sortOrder, m.id")
    List<MenuItem> findAvailableWithCategory();

    @Query("select m from MenuItem m join fetch m.category c order by c.sortOrder, m.id")
    List<MenuItem> findAllWithCategory();
}
