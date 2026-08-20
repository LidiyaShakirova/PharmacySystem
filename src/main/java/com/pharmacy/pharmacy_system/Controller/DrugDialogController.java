package com.pharmacy.pharmacy_system.Controller;

import com.pharmacy.pharmacy_system.Entity.Drug;
import com.pharmacy.pharmacy_system.Entity.DrugCategory;
import com.pharmacy.pharmacy_system.Service.DrugCategoryService;
import com.pharmacy.pharmacy_system.Service.DrugService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DrugDialogController {

    private final DrugService drugService;
    private final DrugCategoryService categoryService;

    @FXML private TextField nameField;
    @FXML private ComboBox<DrugCategory> categoryComboBox;
    @FXML private TextField weeklySalesField;
    @FXML private TextField currentStockField;
    @FXML private Button saveButton;

    private Drug drug;

    @FXML
    public void initialize() {

        List<DrugCategory> categories = categoryService.findAllOrderedByPriority();
        categoryComboBox.setItems(FXCollections.observableArrayList(categories));
        categoryComboBox.setCellFactory(param -> new ListCell<DrugCategory>() {
            @Override
            protected void updateItem(DrugCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? null : item.getName());
            }
        });
        categoryComboBox.setButtonCell(new ListCell<DrugCategory>() {
            @Override
            protected void updateItem(DrugCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? null : item.getName());
            }
        });

        // Ограничение ввода только цифр
        weeklySalesField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) weeklySalesField.setText(newVal.replaceAll("[^\\d]", ""));
        });
        currentStockField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) currentStockField.setText(newVal.replaceAll("[^\\d]", ""));
        });
    }

    public void setDrug(Drug drug) {
        this.drug = drug;
        if (drug != null) {
            nameField.setText(drug.getName());
            categoryComboBox.setValue(drug.getCategory());
            weeklySalesField.setText(String.valueOf(drug.getWeeklySales()));
            currentStockField.setText(String.valueOf(drug.getCurrentStock()));
        }
    }

    @FXML
    private void saveDrug() {
        String name = nameField.getText().trim();
        DrugCategory category = categoryComboBox.getValue();
        String weeklySalesText = weeklySalesField.getText().trim();
        String currentStockText = currentStockField.getText().trim();

        if (name.isEmpty()) {
            showAlert("Ошибка", "Название препарата не может быть пустым");
            return;
        }
        if (category == null) {
            showAlert("Ошибка", "Выберите категорию");
            return;
        }
        if (weeklySalesText.isEmpty()) {
            showAlert("Ошибка", "Введите недельный расход");
            return;
        }
        if (currentStockText.isEmpty()) {
            showAlert("Ошибка", "Введите текущий остаток");
            return;
        }

        int weeklySales = Integer.parseInt(weeklySalesText);
        int currentStock = Integer.parseInt(currentStockText);

        try {
            if (drug == null) {
                drugService.createDrug(name, category.getId(), weeklySales, currentStock);
            } else {
                drugService.updateDrug(drug.getId(), name, category.getId(), weeklySales, currentStock);
            }
            close();
        } catch (IllegalArgumentException e) {
            showAlert("Ошибка", e.getMessage());
        }
    }

    @FXML
    private void close() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}