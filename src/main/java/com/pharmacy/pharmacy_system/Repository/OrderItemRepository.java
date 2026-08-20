package com.pharmacy.pharmacy_system.Repository;

import com.pharmacy.pharmacy_system.Entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Repository

    public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

        List<OrderItem> findByOrderId(Long orderId);
        Optional<OrderItem> findByOrderIdAndDrugId(Long orderId, Long drugId);
        @Modifying
        @Transactional
        void deleteByOrderId(Long orderId);


}
