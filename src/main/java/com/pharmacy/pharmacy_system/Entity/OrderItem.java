package com.pharmacy.pharmacy_system.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "drug_id", nullable = false)
    private Drug drug;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "received_quantity")
    private Integer receivedQuantity;

    @Transient
    private Integer comingQuantity;

}
