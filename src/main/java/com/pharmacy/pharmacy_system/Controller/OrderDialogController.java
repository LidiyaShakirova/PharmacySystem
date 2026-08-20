package com.pharmacy.pharmacy_system.Controller;

import com.pharmacy.pharmacy_system.Entity.Drug;
import com.pharmacy.pharmacy_system.Entity.Order;
import com.pharmacy.pharmacy_system.Entity.OrderItem;
import com.pharmacy.pharmacy_system.Service.DrugService;
import com.pharmacy.pharmacy_system.Service.OrderService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDialogController {

    private final DrugService drugService;
    private final OrderService orderService;

    @FXML
    private ComboBox<Drug> drugComboBox;
    @FXML
    private TextField quantityField;

    @FXML
    private TableView<OrderItem> itemsTable;
    @FXML
    private TableColumn<OrderItem, String> drugNameColumn;
    @FXML
    private TableColumn<OrderItem, Integer> quantityColumn;

    @FXML
    private Button saveButton;

    private ObservableList<OrderItem> items = FXCollections.observableArrayList();
    private Order editingOrder;
    private List<Drug> allDrugs;

    @FXML
    public void initialize() {
        log.info("OrderDialogController initialize");

        // Загружаем ВСЕ препараты
        loadAllDrugs();

        // Настройка колонок
        drugNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDrug().getName()));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        itemsTable.setItems(items);

        // Настройка ComboBox - показываем название и остаток
        drugComboBox.setCellFactory(param -> new ListCell<Drug>() {
            @Override
            protected void updateItem(Drug item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (остаток: " + item.getCurrentStock() + ")");
                }
            }
        });

        drugComboBox.setButtonCell(new ListCell<Drug>() {
            @Override
            protected void updateItem(Drug item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        quantityField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("\\d*")) {
                quantityField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }

    private void loadAllDrugs() {
        try {
            allDrugs = drugService.findAll();
            // Сортируем по алфавиту
            allDrugs.sort(Comparator.comparing(Drug::getName, String.CASE_INSENSITIVE_ORDER));
            drugComboBox.setItems(FXCollections.observableArrayList(allDrugs));
            log.info("Загружено {} препаратов", allDrugs.size());
        } catch (Exception e) {
            log.error("Ошибка загрузки препаратов", e);
        }
    }

    public void setOrder(Order order) {
        this.editingOrder = order;
        if (order != null) {
            items.setAll(order.getItems());
        } else {
            items.clear();
        }
    }

    @FXML
    private void addItem() {
        Drug selectedDrug = drugComboBox.getValue();
        String quantityText = quantityField.getText().trim();

        if (selectedDrug == null) {
            showAlert("Ошибка", "Выберите препарат");
            return;
        }

        if (quantityText.isEmpty()) {
            showAlert("Ошибка", "Введите количество");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            showAlert("Ошибка", "Количество должно быть целым числом");
            return;
        }

        if (quantity <= 0) {
            showAlert("Ошибка", "Количество должно быть положительным");
            return;
        }

        boolean alreadyExists = items.stream()
                .anyMatch(item -> item.getDrug().getId().equals(selectedDrug.getId()));

        if (alreadyExists) {
            showAlert("Ошибка", "Препарат уже добавлен в заявку");
            return;
        }

        OrderItem newItem = new OrderItem();
        newItem.setDrug(selectedDrug);
        newItem.setQuantity(quantity);
        items.add(newItem);

        // Очищаем поля
        drugComboBox.setValue(null);
        quantityField.clear();

        log.info("Добавлен препарат: {} x {}", selectedDrug.getName(), quantity);
    }

    @FXML
    private void removeItem() {
        OrderItem selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            items.remove(selected);
            log.info("Удалён препарат: {}", selected.getDrug().getName());
        } else {
            showAlert("Ошибка", "Выберите позицию для удаления");
        }
    }

    @FXML
    private void saveOrder() {
        if (items.isEmpty()) {
            showAlert("Ошибка", "Добавьте хотя бы один препарат");
            return;
        }

        List<OrderService.OrderItemDto> dtos = new ArrayList<>();
        for (OrderItem item : items) {
            dtos.add(new OrderService.OrderItemDto(item.getDrug().getId(), item.getQuantity()));
        }

        try {
            if (editingOrder == null) {
                orderService.createManualOrder(dtos);
                showInfo("Успех", "Заявка создана");
            } else {
                orderService.updateDraftOrder(editingOrder.getId(), dtos);
                showInfo("Успех", "Заявка обновлена");
            }
            close();
        } catch (Exception e) {
            log.error("Ошибка", e);
            showAlert("Ошибка", e.getMessage());
        }
    }

    @FXML
    private void close() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}