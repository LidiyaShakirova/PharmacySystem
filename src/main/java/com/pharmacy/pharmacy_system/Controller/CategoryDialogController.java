package com.pharmacy.pharmacy_system.Controller;

import com.pharmacy.pharmacy_system.Entity.DrugCategory;
import com.pharmacy.pharmacy_system.Service.DrugCategoryService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryDialogController {

    private final DrugCategoryService categoryService;

    @FXML
    private TextField nameField;
    @FXML private TextField priorityField;
    @FXML private TextField minStockField;
    @FXML private Button saveButton;

    private DrugCategory category;

    @FXML
    public void initialize() {

        priorityField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) priorityField.setText(newVal.replaceAll("[^\\d]", ""));
        });
        minStockField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) minStockField.setText(newVal.replaceAll("[^\\d]", ""));
        });
    }

    public void setCategory(DrugCategory category) {
        this.category = category;
        if (category != null) {
            nameField.setText(category.getName());
            priorityField.setText(String.valueOf(category.getPriority()));
            minStockField.setText(category.getMinStock() == null ? "" : String.valueOf(category.getMinStock()));
        }
    }

    @FXML
    private void saveCategory() {
        String name = nameField.getText().trim();
        String priorityText = priorityField.getText().trim();
        String minStockText = minStockField.getText().trim();

        if (name.isEmpty()) {
            showAlert("Ошибка", "Название категории не может быть пустым");
            return;
        }
        if (priorityText.isEmpty()) {
            showAlert("Ошибка", "Приоритет не может быть пустым");
            return;
        }
        int priority = Integer.parseInt(priorityText);
        Integer minStock = minStockText.isEmpty() ? null : Integer.parseInt(minStockText);

        try {
            if (category == null) {
                categoryService.createCategory(name, priority, minStock);
            } else {
                categoryService.updateCategory(category.getId(), name, priority, minStock);
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
