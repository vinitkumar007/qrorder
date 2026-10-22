package com.hotel.qrorder.repo;

import com.hotel.qrorder.model.DiningTable;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiningTableRepo extends JpaRepository<DiningTable, Long> {
    Optional<DiningTable> findByQrTokenAndActiveTrue(String qrToken);
    boolean existsByTableNumber(Integer tableNumber);
    List<DiningTable> findAllByOrderByTableNumberAsc();

    /** Payment ke time table row lock hoti hai, taaki do log ek saath bill settle na kar sakein. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from DiningTable t where t.id = :id")
    Optional<DiningTable> findForUpdate(@Param("id") Long id);
}
