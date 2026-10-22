package com.hotel.qrorder.repo;

import com.hotel.qrorder.model.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepo extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderBySortOrderAsc();
}
