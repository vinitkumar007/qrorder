package com.hotel.qrorder.repo;

import com.hotel.qrorder.model.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepo extends JpaRepository<Payment, Long> {
    @Query("select p from Payment p join fetch p.table where p.id = :id")
    Optional<Payment> findWithTable(@Param("id") Long id);
}
