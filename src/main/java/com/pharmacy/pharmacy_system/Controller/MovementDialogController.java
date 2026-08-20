package com.pharmacy.pharmacy_system.Controller;

import com.pharmacy.pharmacy_system.Entity.Drug;
import com.pharmacy.pharmacy_system.Service.DrugService;
import com.pharmacy.pharmacy_system.Service.StockMovementService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;

import java.util.stream.Collectors;

import javafx.scene.control.TextField;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovementDialogController {

    private final StockMovementService movementService;
    private final DrugService drugService;

    @FXML private TextField drugSearchField;
    @FXML private TextField quantityField;
    @FXML private Label typeLabel;

    @FXML private Button closeButton;

    @FXML private TableView<ItemEntry> itemsTable;
    @FXML private TableColumn<ItemEntry, String> drugColumn;
    @FXML private TableColumn<ItemEntry, Integer> quantityColumn;
    @FXML private TableColumn<ItemEntry, Integer> remainingColumn;
    @FXML private Label totalItemsLabel;
    @FXML private Label totalQuantityLabel;

    private String movementType;
    private List<Drug> allDrugs;
    private List<Drug> filteredDrugs = new java.util.ArrayList<>();
    private Popup searchPopup;
    private ListView<String> drugListView;
    private boolean popupVisible = false;
    private ObservableList<ItemEntry> items = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        log.info("MovementDialogController initialize");

        loadAllDrugs();
        createSearchPopup();

        drugColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDrug().getName()));
        quantityColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getQuantity()).asObject());
        remainingColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getDrug().getCurrentStock()).asObject());
        itemsTable.setItems(items);


        drugSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterDrugs(newVal);
            showPopup();
        });

        drugSearchField.setOnMouseClicked(e -> {
            filterDrugs(drugSearchField.getText());
            showPopup();
        });

        drugSearchField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                filterDrugs(drugSearchField.getText());
                showPopup();
            }
        });

        drugSearchField.setOnKeyPressed(this::handleKeyPress);

        // Только цифры
        quantityField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("\\d*")) {
                quantityField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }

    private void createSearchPopup() {
        drugListView = new ListView<>();
        drugListView.setPrefWidth(430);
        drugListView.setPrefHeight(200);
        drugListView.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #cecece;
            -fx-border-radius: 4;
            -fx-background-radius: 4;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);
            """);

        drugListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    setText(item);
                    setStyle("-fx-padding: 8 12;");
                } else {
                    setText(null);
                }
            }
        });

        drugListView.setOnMouseClicked(e -> {
            String selected = drugListView.getSelectionModel().getSelectedItem();
            if (selected != null) selectDrug(selected);
        });

        searchPopup = new Popup();
        searchPopup.getContent().add(drugListView);
        searchPopup.setAutoHide(true);
        searchPopup.setAutoFix(true);
        searchPopup.setOnHidden(e -> popupVisible = false);
    }

    private void filterDrugs(String search) {
        if (search == null || search.trim().isEmpty()) {
            filteredDrugs.clear();
            filteredDrugs.addAll(allDrugs);
        } else {
            String lowerSearch = search.toLowerCase().trim();
            filteredDrugs = allDrugs.stream()
                    .filter(drug -> drug.getName().toLowerCase().contains(lowerSearch))
                    .sorted((d1, d2) -> {
                        boolean d1StartsWith = d1.getName().toLowerCase().startsWith(lowerSearch);
                        boolean d2StartsWith = d2.getName().toLowerCase().startsWith(lowerSearch);
                        if (d1StartsWith && !d2StartsWith) return -1;
                        if (!d1StartsWith && d2StartsWith) return 1;
                        return d1.getName().compareToIgnoreCase(d2.getName());
                    })
                    .collect(Collectors.toList());
        }
        updateDrugListView();
    }

    private void updateDrugListView() {
        ObservableList<String> itemsList = FXCollections.observableArrayList();
        filteredDrugs.forEach(drug ->
                itemsList.add(drug.getName() + " (остаток: " + drug.getCurrentStock() + ")")
        );
        drugListView.setItems(itemsList);

        if (!filteredDrugs.isEmpty()) {
            drugListView.getSelectionModel().select(0);
            drugListView.scrollTo(0);
        }
    }

    private void showPopup() {
        if (!popupVisible) {
            popupVisible = true;
            Bounds bounds = drugSearchField.localToScreen(drugSearchField.getBoundsInLocal());
            searchPopup.show(drugSearchField, bounds.getMinX(), bounds.getMaxY());
        }
    }

    private void hidePopup() {
        if (popupVisible) {
            popupVisible = false;
            searchPopup.hide();
        }
    }

    private void handleKeyPress(KeyEvent e) {
        if (!popupVisible || filteredDrugs.isEmpty()) return;

        switch (e.getCode()) {
            case DOWN:
                e.consume();
                int next = drugListView.getSelectionModel().getSelectedIndex() + 1;
                if (next < filteredDrugs.size()) {
                    drugListView.getSelectionModel().select(next);
                    drugListView.scrollTo(next);
                }
                break;
            case UP:
                e.consume();
                int prev = drugListView.getSelectionModel().getSelectedIndex() - 1;
                if (prev >= 0) {
                    drugListView.getSelectionModel().select(prev);
                    drugListView.scrollTo(prev);
                }
                break;
            case ENTER:
                e.consume();
                String selected = drugListView.getSelectionModel().getSelectedItem();
                if (selected != null) selectDrug(selected);
                break;
            case ESCAPE:
                e.consume();
                hidePopup();
                break;
        }
    }

    private void selectDrug(String displayName) {
        String pureName = extractPureName(displayName);
        drugSearchField.setText(pureName);
        hidePopup();
        log.info("Выбран препарат: {}", pureName);
    }

    private String extractPureName(String displayName) {
        if (displayName == null) return "";
        int idx = displayName.indexOf(" (остаток:");
        return idx > 0 ? displayName.substring(0, idx) : displayName;
    }

    private Drug findDrugByName(String name) {
        if (name == null || name.isEmpty()) return null;
        String pureName = extractPureName(name).trim();
        return allDrugs.stream()
                .filter(d -> d.getName().equalsIgnoreCase(pureName))
                .findFirst()
                .orElse(null);
    }

    private void loadAllDrugs() {
        try {
            allDrugs = drugService.findAll();
            allDrugs.sort(Comparator.comparing(Drug::getName, String.CASE_INSENSITIVE_ORDER));
            log.info("Загружено {} препаратов", allDrugs.size());
        } catch (Exception e) {
            log.error("Ошибка загрузки", e);
        }
    }

    public void setMovementType(String type) {
        this.movementType = type;
        typeLabel.setText(type.equals("Приход") ? "📦 Приход товара" : "📤 Расход товара");
        drugSearchField.clear();
        quantityField.clear();
        items.clear();
        updateTotals();
        hidePopup();
    }

    @FXML
    private void addToList() {
        String selectedText = drugSearchField.getText().trim();

        if (selectedText.isEmpty()) {
            showAlert("Ошибка", "Введите или выберите препарат");
            return;
        }

        Drug selectedDrug = findDrugByName(selectedText);
        if (selectedDrug == null) {
            showAlert("Ошибка", "Препарат не найден");
            return;
        }

        String quantityText = quantityField.getText().trim();
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

        if ("Расход".equals(movementType) && selectedDrug.getCurrentStock() < quantity) {
            showAlert("Ошибка", "Недостаточно остатка!\nДоступно: " + selectedDrug.getCurrentStock());
            return;
        }

        // Проверка на дубликаты
        for (ItemEntry item : items) {
            if (item.getDrug().getId().equals(selectedDrug.getId())) {
                showAlert("Ошибка", "Препарат уже добавлен в список");
                return;
            }
        }

        items.add(new ItemEntry(selectedDrug, quantity));
        updateTotals();


        drugSearchField.clear();
        quantityField.clear();
        hidePopup();
    }

    @FXML
    private void removeFromList() {
        ItemEntry selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            items.remove(selected);
            updateTotals();
        } else {
            showAlert("Ошибка", "Выберите позицию для удаления");
        }
    }

    private void updateTotals() {
        totalItemsLabel.setText(String.valueOf(items.size()));
        int total = items.stream().mapToInt(ItemEntry::getQuantity).sum();
        totalQuantityLabel.setText(String.valueOf(total));
    }

    @FXML
    private void saveMovement() {
        if (items.isEmpty()) {
            showAlert("Ошибка", "Добавьте хотя бы один препарат в список");
            return;
        }

        try {
            for (ItemEntry item : items) {
                if ("Приход".equals(movementType)) {
                    movementService.registerIncome(item.getDrug().getName(), item.getQuantity());
                } else {
                    if (item.getDrug().getCurrentStock() < item.getQuantity()) {
                        showAlert("Ошибка", "Недостаточно остатка для: " + item.getDrug().getName() +
                                "\nДоступно: " + item.getDrug().getCurrentStock());
                        return;
                    }
                    movementService.registerExpense(item.getDrug().getName(), item.getQuantity());
                }
            }
            showInfo("Успех", movementType + " зарегистрирован для " + items.size() + " препаратов");
            closeDialog();
        } catch (Exception e) {
            log.error("Ошибка", e);
            showAlert("Ошибка", e.getMessage());
        }
    }

    @FXML
    private void closeDialog() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
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

    public static class ItemEntry {
        private final Drug drug;
        private final int quantity;

        public ItemEntry(Drug drug, int quantity) {
            this.drug = drug;
            this.quantity = quantity;
        }

        public Drug getDrug() { return drug; }
        public int getQuantity() { return quantity; }
    }
}

