package com.hotel.qrorder.repo;

import com.hotel.qrorder.model.StaffUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffUserRepo extends JpaRepository<StaffUser, Long> {
    Optional<StaffUser> findByUsername(String username);
    List<StaffUser> findAllByOrderByUsernameAsc();
    long countByRole(String role);
}
