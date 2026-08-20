package com.pharmacy.pharmacy_system.Repository;


import com.pharmacy.pharmacy_system.Entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {
        Optional<AppSetting> findByKey(String key);

}
