package com.pharmacy.pharmacy_system.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "drug_categories")
@Data
public class DrugCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer priority;
    @Override
    public String toString() {
        return name;
    }


    @Column(name = "min_stock")
    private Integer minStock;


}
