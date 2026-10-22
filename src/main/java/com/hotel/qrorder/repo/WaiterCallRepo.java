package com.hotel.qrorder.repo;

import com.hotel.qrorder.model.CallStatus;
import com.hotel.qrorder.model.WaiterCall;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WaiterCallRepo extends JpaRepository<WaiterCall, Long> {
    @Query("select w from WaiterCall w join fetch w.table where w.status = :st order by w.createdAt")
    List<WaiterCall> findByStatusWithTable(@Param("st") CallStatus st);

    @Query("select w from WaiterCall w join fetch w.table where w.id = :id")
    Optional<WaiterCall> findWithTable(@Param("id") Long id);
}
