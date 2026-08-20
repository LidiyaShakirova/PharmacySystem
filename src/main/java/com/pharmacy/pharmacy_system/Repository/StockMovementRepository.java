package com.pharmacy.pharmacy_system.Repository;

import com.pharmacy.pharmacy_system.Entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findAllByOrderByMovementDateDesc();
    @Query("SELECT sm FROM StockMovement sm WHERE LOWER(sm.drug.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<StockMovement> findByDrugName(@Param("name") String name);

    List<StockMovement> findByMovementDateBetween(LocalDateTime start, LocalDateTime end);

    List<StockMovement> findByTypeAndMovementDateBetween(String type, LocalDateTime start, LocalDateTime end);
}