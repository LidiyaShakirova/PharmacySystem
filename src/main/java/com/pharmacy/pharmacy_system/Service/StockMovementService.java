package com.pharmacy.pharmacy_system.Service;

import com.pharmacy.pharmacy_system.Entity.Drug;
import com.pharmacy.pharmacy_system.Entity.StockMovement;
import com.pharmacy.pharmacy_system.Entity.User;
import com.pharmacy.pharmacy_system.Repository.DrugRepository;
import com.pharmacy.pharmacy_system.Repository.StockMovementRepository;
import com.pharmacy.pharmacy_system.Util.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockMovementService {
    private final StockMovementRepository stockMovementRepository;
    private final DrugRepository drugRepository;


    @Transactional
    public StockMovement registerIncome(String drugName, Integer quantity) {
        return registerMovement(drugName, "Приход", quantity);
    }

    @Transactional
    public StockMovement registerExpense(String drugName, Integer quantity) {
        return registerMovement(drugName, "Расход", quantity);
    }

    @Transactional
    public StockMovement registerMovement(String drugName, String type, Integer quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть положительным");
        }
        if (drugName == null || drugName.trim().isEmpty()) {
            throw new IllegalArgumentException("Название препарата не может быть пустым");
        }

        Drug drug = drugRepository.findByNameIgnoreCase(drugName.trim())
                .orElseThrow(() -> new IllegalArgumentException("Препарат с названием '" + drugName + "' не найден"));

        int newStock;
        if ("Приход".equals(type)) {
            newStock = drug.getCurrentStock() + quantity;
        } else if ("Расход".equals(type)) {
            if (drug.getCurrentStock() < quantity) {
                throw new IllegalArgumentException(
                        "Недостаточно остатка для списания. Доступно: " + drug.getCurrentStock());
            }
            newStock = drug.getCurrentStock() - quantity;
        } else {
            throw new IllegalArgumentException("Неверный тип движения: " + type);
        }

        drug.setCurrentStock(newStock);
        drugRepository.save(drug);

        User currentUser = UserSession.getInstance().getCurrentUser();

        StockMovement movement = new StockMovement();
        movement.setDrug(drug);
        movement.setUser(currentUser);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setMovementDate(LocalDateTime.now());

        StockMovement saved = stockMovementRepository.save(movement);
        log.info("Зарегистрировано движение: {} {} ед. препарата '{}' пользователем {}",
                type, quantity, drug.getName(), currentUser.getUsername());
        return saved;
    }

    public List<StockMovement> findAll() {
        return stockMovementRepository.findAllByOrderByMovementDateDesc();
    }

    public List<StockMovement> findByDrugName(String drugName) {
        if (drugName == null || drugName.trim().isEmpty()) {
            return List.of();
        }
        return stockMovementRepository.findByDrugName(drugName.trim());
    }

    public List<StockMovement> findByDateBetween(LocalDateTime start, LocalDateTime end) {
        return stockMovementRepository.findByMovementDateBetween(start, end);
    }

    public List<StockMovement> findByTypeAndDateBetween(String type, LocalDateTime start, LocalDateTime end) {
        return stockMovementRepository.findByTypeAndMovementDateBetween(type, start, end);
    }
}



