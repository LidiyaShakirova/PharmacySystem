package com.pharmacy.pharmacy_system.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "app_settings")
@Data
public class AppSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "param_key", unique = true, nullable = false)
    private String key;

    @Column(name = "param_value")
    private String value;
}