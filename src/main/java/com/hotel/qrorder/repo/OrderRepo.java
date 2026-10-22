package com.hotel.qrorder.repo;

import com.hotel.qrorder.model.DiningTable;
import com.hotel.qrorder.model.OrderEntity;
import com.hotel.qrorder.model.OrderStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepo extends JpaRepository<OrderEntity, Long> {
    String FETCH = "select distinct o from OrderEntity o join fetch o.table t left join fetch o.items i left join fetch i.menuItem ";

    @Query(FETCH + "where o.status in :statuses order by o.createdAt")
    List<OrderEntity> findActive(@Param("statuses") Collection<OrderStatus> statuses);

    @Query(FETCH + "where o.id = :id")
    Optional<OrderEntity> findWithItems(@Param("id") Long id);

    @Query(FETCH + "where t.id = :tableId and o.paymentId is null and o.status <> :cancelled order by o.createdAt")
    List<OrderEntity> findUnpaidByTable(@Param("tableId") Long tableId, @Param("cancelled") OrderStatus cancelled);

    @Query(FETCH + "where o.paymentId is null and o.status <> :cancelled order by t.tableNumber, o.createdAt")
    List<OrderEntity> findAllUnpaid(@Param("cancelled") OrderStatus cancelled);

    @Query(FETCH + "where o.paymentId = :pid order by o.createdAt")
    List<OrderEntity> findByPayment(@Param("pid") Long pid);

    Optional<OrderEntity> findByIdAndTable(Long id, DiningTable table);
}
