package com.pharmacy.pharmacy_system.Repository;

import com.pharmacy.pharmacy_system.Entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findByStatusOrderByCreatedAtDesc(String status);


    List<Order> findByUserIdAndStatus(Long userId, String status);

    List<Order> findByStatusIn(List<String> statuses);

}