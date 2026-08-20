package com.pharmacy.pharmacy_system.Controller;

import com.pharmacy.pharmacy_system.Entity.Order;
import com.pharmacy.pharmacy_system.Entity.User;
import com.pharmacy.pharmacy_system.Service.AppSettingService;
import com.pharmacy.pharmacy_system.Service.OrderItemService;
import com.pharmacy.pharmacy_system.Service.OrderService;
import com.pharmacy.pharmacy_system.Util.SpringFXMLLoader;
import com.pharmacy.pharmacy_system.Util.UserSession;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AppSettingService appSettingService;

    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Long> idColumn;
    @FXML private TableColumn<Order, String> userColumn;
    @FXML private TableColumn<Order, String> statusColumn;
    @FXML private TableColumn<Order, String> dateColumn;
    @FXML private TableColumn<Order, Integer> itemsCountColumn;

    @FXML private ComboBox<String> statusFilter;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    @FXML private Button createOrderButton;
    @FXML private Button autoOrderButton;
    @FXML private Button deleteOrderButton;
    @FXML private Button viewDetailsButton;

    @FXML private Label targetWeeksLabel;
    @FXML private Label orderLimitLabel;
    @FXML private TextField targetWeeksField;
    @FXML private TextField orderLimitField;
    @FXML private Button saveSettingsButton;

    private ObservableList<Order> ordersData = FXCollections.observableArrayList();
    private FilteredList<Order> filteredData;

    @FXML
    public void initialize() {
        log.info("OrderController initialize");

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        userColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getUser().getUsername()));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
        itemsCountColumn.setCellValueFactory(cellData -> {
            int count = cellData.getValue().getItems() != null ?
                    cellData.getValue().getItems().size() : 0;
            return new SimpleIntegerProperty(count).asObject();
        });


        loadOrders();

        filteredData = new FilteredList<>(ordersData, p -> true);
        ordersTable.setItems(filteredData);

        statusFilter.setItems(FXCollections.observableArrayList("Все", "Черновик", "Согласована", "Закрыта"));
        statusFilter.getSelectionModel().selectFirst();

        // Двойной клик для просмотра деталей
        ordersTable.setRowFactory(tv -> {
            TableRow<Order> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showOrderDetails(row.getItem());
                }
            });
            return row;
        });

        User currentUser = UserSession.getInstance().getCurrentUser();
        boolean isPharmacist = currentUser != null && "PHARMACIST".equals(currentUser.getRole());

        createOrderButton.setVisible(isPharmacist);
        autoOrderButton.setVisible(isPharmacist);
        deleteOrderButton.setVisible(isPharmacist);
        viewDetailsButton.setVisible(true);

        targetWeeksLabel.setVisible(true);
        orderLimitLabel.setVisible(true);
        targetWeeksField.setVisible(isPharmacist);
        orderLimitField.setVisible(isPharmacist);
        saveSettingsButton.setVisible(isPharmacist);

        // Слушатели для автоматического применения фильтров
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        refreshSettingsDisplay();
    }

    public void loadOrders() {
        Platform.runLater(() -> {
            ordersData.setAll(orderService.findMyOrders());
            applyFilters();
            log.info("Загружено {} заявок", ordersData.size());
        });
    }

    @FXML
    private void applyFilters() {
        String selectedStatus = statusFilter.getValue();
        LocalDateTime start = startDatePicker.getValue() != null ?
                startDatePicker.getValue().atStartOfDay() : null;
        LocalDateTime end = endDatePicker.getValue() != null ?
                endDatePicker.getValue().atTime(23, 59, 59) : null;

        filteredData.setPredicate(order -> {
            if (selectedStatus != null && !"Все".equals(selectedStatus)) {
                if (!order.getStatus().equals(selectedStatus)) return false;
            }
            if (start != null && order.getCreatedAt().isBefore(start)) return false;
            if (end != null && order.getCreatedAt().isAfter(end)) return false;
            return true;
        });
        ordersTable.refresh();
    }

    @FXML
    private void resetFilters() {
        statusFilter.getSelectionModel().selectFirst();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        applyFilters();
    }

    @FXML
    private void refreshTable() {
        loadOrders();
    }

    @FXML
    private void createOrder() {
        openOrderDialog(null);
    }

    @FXML
    private void autoOrder() {
        try {
            Order order = orderService.generateAutoOrder();
            if (order != null) {
                showInfo("Успех", "Автоматически создана заявка №" + order.getId());
                loadOrders();
            } else {
                showInfo("Информация", "Нет препаратов, требующих заказа");
            }
        } catch (Exception e) {
            showAlert("Ошибка", e.getMessage());
        }
    }

    @FXML
    private void deleteOrder() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите заявку");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить заявку №" + selected.getId() + "?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    orderService.deleteOrder(selected.getId());
                    loadOrders();
                } catch (Exception e) {
                    showAlert("Ошибка", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void viewDetails() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите заявку");
            return;
        }
        showOrderDetails(selected);
    }

    public void openOrderDialog(Order order) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/orderDialog.fxml"));
            loader.setControllerFactory(SpringFXMLLoader.getControllerFactory());
            Parent root = loader.load();

            OrderDialogController controller = loader.getController();
            controller.setOrder(order);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(order == null ? "Новая заявка" : "Редактирование заявки");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(ordersTable.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

            loadOrders();
        } catch (IOException e) {
            log.error("Ошибка", e);
            showAlert("Ошибка", "Не удалось открыть окно");
        }
    }

    private void showOrderDetails(Order order) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/orderDetailsDialog.fxml"));
            loader.setControllerFactory(SpringFXMLLoader.getControllerFactory());
            Parent root = loader.load();

            OrderDetailsDialogController controller = loader.getController();
            controller.setOrder(order);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Заявка №" + order.getId());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(ordersTable.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

            loadOrders(); // обновить список после закрытия деталей (если статус изменился)
        } catch (IOException e) {
            log.error("Ошибка", e);
            showAlert("Ошибка", "Не удалось открыть детали");
        }
    }

    @FXML
    private void saveSettings() {
        try {
            int weeks = Integer.parseInt(targetWeeksField.getText());
            int limit = Integer.parseInt(orderLimitField.getText());
            if (weeks < 1) throw new IllegalArgumentException("Целевой период должен быть ≥ 1");
            if (limit < 1) throw new IllegalArgumentException("Лимит должен быть ≥ 1");

            appSettingService.setTargetWeeks(weeks);
            appSettingService.setOrderLimit(limit);

            refreshSettingsDisplay();
            showInfo("Настройки сохранены", "Новые значения будут использоваться при автоматическом формировании заявок.");
        } catch (NumberFormatException e) {
            showAlert("Ошибка", "Введите целые числа");
        } catch (IllegalArgumentException e) {
            showAlert("Ошибка", e.getMessage());
        }
    }

    private void refreshSettingsDisplay() {
        int weeks = appSettingService.getTargetWeeks();
        int limit = appSettingService.getOrderLimit();
        targetWeeksLabel.setText("Текущий: " + weeks);
        orderLimitLabel.setText("Текущий: " + limit);
        targetWeeksField.setText(String.valueOf(weeks));
        orderLimitField.setText(String.valueOf(limit));
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