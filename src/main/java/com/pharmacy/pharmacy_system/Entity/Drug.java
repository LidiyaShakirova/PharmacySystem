package com.pharmacy.pharmacy_system.Entity;

import jakarta.persistence.*;

import lombok.Data;

@Entity
@Table(name = "drugs")
@Data
public class Drug {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private DrugCategory category;
    @Column(name = "current_stock")
    private Integer currentStock;
    @Column(name = "weekly_sales")
   private Integer weeklySales;
    @Override
    public String toString() {
        return name;
    }
}
