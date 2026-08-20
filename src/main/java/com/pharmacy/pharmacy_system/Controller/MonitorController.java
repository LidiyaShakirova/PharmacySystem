package com.pharmacy.pharmacy_system.Controller;

import com.pharmacy.pharmacy_system.Entity.Drug;
import com.pharmacy.pharmacy_system.Entity.DrugCategory;
import com.pharmacy.pharmacy_system.Entity.User;
import com.pharmacy.pharmacy_system.Service.DrugCategoryService;
import com.pharmacy.pharmacy_system.Service.DrugService;
import com.pharmacy.pharmacy_system.Util.SpringFXMLLoader;
import com.pharmacy.pharmacy_system.Util.UserSession;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorController {
    private final DrugService drugService;
    private final DrugCategoryService categoryService;

    @FXML private TableView<Drug> drugsTable;
    @FXML private TableColumn<Drug, String> nameColumn;
    @FXML private TableColumn<Drug, String> categoryColumn;
    @FXML private TableColumn<Drug, Integer> stockColumn;
    @FXML private TableColumn<Drug, Integer> salesColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<DrugCategory> categoryFilter;
    @FXML private Label totalCountLabel;

    @FXML private VBox drugAdminGroup;
    @FXML private VBox categoryAdminGroup;

    private ObservableList<Drug> masterData = FXCollections.observableArrayList();
    private FilteredList<Drug> filteredData;
    private SortedList<Drug> sortedData;

    private String currentSearchText = "";
    private DrugCategory currentSelectedCategory = null;
    @FXML
    public void initialize() {
        log.info("MonitorController.initialize() начат");


        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCategory().getName()));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        salesColumn.setCellValueFactory(new PropertyValueFactory<>("weeklySales"));

        // Устанавливаем сортировку по имени по умолчанию (А-Я)
        nameColumn.setSortType(TableColumn.SortType.ASCENDING);
        drugsTable.getSortOrder().add(nameColumn);

        // Загрузка категорий
        loadCategories();

        // Создаём filteredData
        filteredData = new FilteredList<>(masterData, drug -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(drugsTable.comparatorProperty());
        drugsTable.setItems(sortedData);

        // Загрузка препаратов
        loadDrugs();

        // Настройка фильтров
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            currentSearchText = newVal;
            applyFilters();
        });

        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentSelectedCategory = newVal;
            applyFilters();
        });

        updateTotalCount();

        // Проверка роли и скрытие панелей для фармацевта
        User currentUser = UserSession.getInstance().getCurrentUser();
        boolean isAdmin = currentUser != null && "ADMIN".equals(currentUser.getRole());
        

        if (drugAdminGroup != null) {
            drugAdminGroup.setVisible(isAdmin);
            drugAdminGroup.setManaged(isAdmin);
        }
        if (categoryAdminGroup != null) {
            categoryAdminGroup.setVisible(isAdmin);
            categoryAdminGroup.setManaged(isAdmin);
        }
        log.info("MonitorController.initialize() завершён");
    }

    private void loadCategories() {
        List<DrugCategory> cats = categoryService.findAllOrderedByPriority();
        ObservableList<DrugCategory> categories = FXCollections.observableArrayList(cats);

        DrugCategory allCategory = new DrugCategory();
        allCategory.setId(0L);
        allCategory.setName("Все категории");
        allCategory.setPriority(0);
        categories.add(0, allCategory);

        categoryFilter.setItems(categories);
        categoryFilter.getSelectionModel().selectFirst();
        currentSelectedCategory = categoryFilter.getValue();
    }

    private void loadDrugs() {
        List<Drug> drugs = drugService.findAll();

        drugs.sort(Comparator.comparing(Drug::getName, String.CASE_INSENSITIVE_ORDER));

        log.info("Загружено препаратов: {}", drugs.size());
        masterData.setAll(drugs);
        applyFilters();


        Platform.runLater(() -> {
            drugsTable.getSortOrder().clear();
            drugsTable.getSortOrder().add(nameColumn);
            nameColumn.setSortType(TableColumn.SortType.ASCENDING);
            drugsTable.sort();
        });
    }

    private void applyFilters() {
        if (filteredData == null) return;

        String searchText = currentSearchText != null ? currentSearchText.toLowerCase().trim() : "";
        DrugCategory selectedCategory = currentSelectedCategory;

        filteredData.setPredicate(drug -> {
            if (drug == null) return false;

            if (selectedCategory != null && selectedCategory.getId() != 0) {
                if (drug.getCategory() == null) return false;
                if (!drug.getCategory().getId().equals(selectedCategory.getId())) {
                    return false;
                }
            }

            if (!searchText.isEmpty()) {
                String drugName = drug.getName();
                if (drugName == null) return false;
                return drugName.toLowerCase().contains(searchText);
            }

            return true;
        });

        updateTotalCount();
    }

    private void updateTotalCount() {
        if (filteredData != null) {
            totalCountLabel.setText("Найдено: " + filteredData.size());
        }
    }

    public void refreshTable() {
        Platform.runLater(() -> {
            log.info("MonitorController.refreshTable() вызван");
            currentSearchText = searchField.getText();
            currentSelectedCategory = categoryFilter.getValue();
            loadDrugs();
            drugsTable.refresh();
        });
    }


    @FXML
    private void addCategory() {
        openCategoryDialog(null);
    }

    @FXML
    private void editCategory() {
        DrugCategory selected = categoryFilter.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getId() == 0) {
            showAlert("Ошибка", "Выберите категорию");
            return;
        }
        openCategoryDialog(selected);
    }

    @FXML
    private void deleteCategory() {
        DrugCategory selected = categoryFilter.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getId() == 0) {
            showAlert("Ошибка", "Выберите категорию");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить категорию '" + selected.getName() + "'?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    categoryService.deleteCategory(selected.getId());
                    loadCategories();
                    refreshTable();
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось удалить категорию");
                }
            }
        });
    }

    private void openCategoryDialog(DrugCategory category) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/categoryDialog.fxml"));
            loader.setControllerFactory(SpringFXMLLoader.getControllerFactory());
            Parent root = loader.load();

            CategoryDialogController controller = loader.getController();
            controller.setCategory(category);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(category == null ? "Новая категория" : "Редактирование категории");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(drugsTable.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

            loadCategories();
            refreshTable();
        } catch (IOException e) {
            log.error("Ошибка", e);
            showAlert("Ошибка", "Не удалось открыть окно");
        }
    }


    @FXML
    private void addDrug() {
        openDrugDialog(null);
    }

    @FXML
    private void editDrug() {
        Drug selected = drugsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите препарат");
            return;
        }
        openDrugDialog(selected);
    }

    @FXML
    private void deleteDrug() {
        Drug selected = drugsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите препарат");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить препарат '" + selected.getName() + "'?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    drugService.deleteDrug(selected.getId());
                    refreshTable();
                } catch (Exception e) {
                    showAlert("Ошибка", "Не удалось удалить препарат");
                }
            }
        });
    }

    private void openDrugDialog(Drug drug) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/drugDialog.fxml"));
            loader.setControllerFactory(SpringFXMLLoader.getControllerFactory());
            Parent root = loader.load();

            DrugDialogController controller = loader.getController();
            controller.setDrug(drug);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(drug == null ? "Новый препарат" : "Редактирование препарата");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(drugsTable.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

            refreshTable();
        } catch (IOException e) {
            log.error("Ошибка", e);
            showAlert("Ошибка", "Не удалось открыть окно");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}



