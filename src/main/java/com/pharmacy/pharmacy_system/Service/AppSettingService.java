package com.pharmacy.pharmacy_system.Service;

import com.pharmacy.pharmacy_system.Entity.AppSetting;
import com.pharmacy.pharmacy_system.Repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppSettingService {
    private final AppSettingRepository settingRepository;

    public int getTargetWeeks() {
        return settingRepository.findByKey("target.weeks")
                .map(s -> Integer.parseInt(s.getValue()))
                .orElse(2);
    }

    public int getOrderLimit() {
        return settingRepository.findByKey("order.limit")
                .map(s -> Integer.parseInt(s.getValue()))
                .orElse(50);
    }

    public void setTargetWeeks(int weeks) {
        updateSetting("target.weeks", String.valueOf(weeks));
    }

    public void setOrderLimit(int limit) {
        updateSetting("order.limit", String.valueOf(limit));
    }

    private void updateSetting(String key, String value) {
        AppSetting setting = settingRepository.findByKey(key)
                .orElse(new AppSetting());
        setting.setKey(key);
        setting.setValue(value);
        settingRepository.save(setting);
    }
}