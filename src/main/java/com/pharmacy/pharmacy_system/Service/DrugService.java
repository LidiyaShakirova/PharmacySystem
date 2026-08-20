package com.pharmacy.pharmacy_system.Service;

import com.pharmacy.pharmacy_system.Entity.Drug;
import com.pharmacy.pharmacy_system.Entity.DrugCategory;
import com.pharmacy.pharmacy_system.Entity.User;
import com.pharmacy.pharmacy_system.Repository.DrugRepository;
import com.pharmacy.pharmacy_system.Util.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DrugService {
    private final DrugRepository drugRepository;
    private final DrugCategoryService categoryService;

    public List<Drug> findAll() {
        return drugRepository.findAll();
    }

    public Drug findById(Long id) {
        return drugRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Препарат с id " + id + " не найден"));
    }

    @Transactional
    public Drug createDrug(String nm, Long categoryId, Integer weeklySales, Integer currentStock) {
        if (nm == null || nm.trim().isEmpty()) {
            throw new IllegalArgumentException("Наименование препарата не может быть пустым");
        }
        DrugCategory category = categoryService.findById(categoryId);

        Drug drug = new Drug();
        drug.setName(nm.trim());
        drug.setCategory(category);
        drug.setWeeklySales(weeklySales != null ? weeklySales : 0);
        drug.setCurrentStock(currentStock != null ? currentStock : 0);

        Drug saved = drugRepository.save(drug);
        log.info("Создан препарат: id={}, nm={}", saved.getId(), saved.getName());
        return saved;
    }
    @Transactional
    public Drug updateDrug(Long id, String nm, Long categoryId, Integer weeklySales, Integer currentStock) {
        Drug drug = findById(id);

        if (nm != null && !nm.trim().isEmpty()) {
            drug.setName(nm.trim());
        }
        if (categoryId != null) {
            DrugCategory category = categoryService.findById(categoryId);
            drug.setCategory(category);
        }
        if (weeklySales != null) {
            drug.setWeeklySales(weeklySales);
        }
        if (currentStock != null) {
            drug.setCurrentStock(currentStock);
        }

        Drug updated = drugRepository.save(drug);
        log.info("Обновлён препарат id={}", id);
        return updated;
    }

    @Transactional
    public void deleteDrug(Long id) {
        Drug drug = findById(id);
        drugRepository.delete(drug);
        log.info("Удалён препарат id={}, name={}", id, drug.getName());
    }

}