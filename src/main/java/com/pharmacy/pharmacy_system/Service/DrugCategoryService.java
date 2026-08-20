package com.pharmacy.pharmacy_system.Service;


import com.pharmacy.pharmacy_system.Entity.DrugCategory;
import com.pharmacy.pharmacy_system.Entity.User;
import com.pharmacy.pharmacy_system.Repository.DrugCategoryRepository;
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
public class DrugCategoryService {

    private final DrugCategoryRepository categoryRepository;

    public List<DrugCategory> findAllOrderedByPriority() {
        return categoryRepository.findAllByOrderByPriorityAsc();
    }

    public List<DrugCategory> findByPriority(Integer priority) {
        return categoryRepository.findByPriority(priority);
    }

    public DrugCategory findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категория с id " + id + " не найдена"));
    }

    // ==================== Изменение (только ADMIN) ====================

    private void checkAdmin() {
        User current = UserSession.getInstance().getCurrentUser();
        if (current == null || !"ADMIN".equals(current.getRole())) {
            throw new SecurityException("Доступ запрещён. Требуется роль ADMIN");
        }
    }

    @Transactional
    public DrugCategory createCategory(String name, Integer priority, Integer minStock) {
        checkAdmin();

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название категории не может быть пустым");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Приоритет не может быть пустым");
        }
        // Валидация minStock и maxStock (можно разрешить null, если необязательны)
        if (minStock != null && minStock <= 0) {
            throw new IllegalArgumentException("Минимальный запас должен быть положительным числом");
        }


        DrugCategory category = new DrugCategory();
        category.setName(name.trim());
        category.setPriority(priority);
        category.setMinStock(minStock);


        DrugCategory saved = categoryRepository.save(category);
        log.info("Создана категория: id={}, name={}, priority={}, minStock={}",
                saved.getId(), saved.getName(), saved.getPriority(), saved.getMinStock());
        return saved;
    }

    @Transactional
    public DrugCategory updateCategory(Long id, String newName, Integer newPriority, Integer newMinStock) {
        checkAdmin();

        DrugCategory category = findById(id);

        if (newName != null && !newName.trim().isEmpty()) {
            category.setName(newName.trim());
        }
        if (newPriority != null) {
            category.setPriority(newPriority);
        }
        if (newMinStock != null) {
            if (newMinStock <= 0) {
                throw new IllegalArgumentException("Минимальный запас должен быть положительным числом");
            }
            category.setMinStock(newMinStock);

        }

        DrugCategory updated = categoryRepository.save(category);
        log.info("Обновлена категория id={}", id, category.getName());
        return updated;
    }

    @Transactional
    public void deleteCategory(Long id) {
        checkAdmin();

        DrugCategory category = findById(id);
        categoryRepository.delete(category);
        log.info("Удалена категория id={}, name={}", id, category.getName());
    }
}