package com.pharmacy.pharmacy_system.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Data
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "drug_id")
    private Drug drug;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String type;

    private Integer quantity;

    @Column(name = "movement_date")
    private LocalDateTime movementDate = LocalDateTime.now();
}