package com.pharmacy.pharmacy_system.Controller;

import com.pharmacy.pharmacy_system.Entity.Order;
import com.pharmacy.pharmacy_system.Entity.StockMovement;
import com.pharmacy.pharmacy_system.Entity.User;
import com.pharmacy.pharmacy_system.Service.OrderService;
import com.pharmacy.pharmacy_system.Service.StockMovementService;
import com.pharmacy.pharmacy_system.Util.SpringFXMLLoader;
import com.pharmacy.pharmacy_system.Util.UserSession;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.List;
import java.util.Optional;
@Slf4j
@Component
@RequiredArgsConstructor
public class MovementController {

    private final StockMovementService movementService;
    private final OrderService orderService;

    @FXML
    private TableView<StockMovement> movementsTable;

    @FXML
    private TableColumn<StockMovement, String> drugColumn;

    @FXML
    private TableColumn<StockMovement, String> typeColumn;

    @FXML
    private TableColumn<StockMovement, Integer> quantityColumn;

    @FXML
    private TableColumn<StockMovement, String> userColumn;

    @FXML
    private TableColumn<StockMovement, String> dateColumn;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TextField searchField;

    @FXML
    private Button incomeButton;

    @FXML
    private Button expenseButton;

    @FXML
    private Button receiveByOrderButton;

    private ObservableList<StockMovement> movementsData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        log.info("MovementController.initialize() начат");

        drugColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDrug().getName()));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        userColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getUser().getUsername()));
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getMovementDate()
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));

        loadMovements();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                loadMovements();
            } else {
                movementsData.setAll(movementService.findByDrugName(newVal.trim()));
                movementsTable.setItems(movementsData);
            }
        });

        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> filterByDate());
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> filterByDate());


        User current = UserSession.getInstance().getCurrentUser();
        if (current != null && "ADMIN".equals(current.getRole())) {
            incomeButton.setVisible(false);
            incomeButton.setManaged(false);
            expenseButton.setVisible(false);
            expenseButton.setManaged(false);
            receiveByOrderButton.setVisible(false);
            receiveByOrderButton.setManaged(false);
        } else {
            incomeButton.setVisible(true);
            incomeButton.setManaged(true);
            expenseButton.setVisible(true);
            expenseButton.setManaged(true);
            receiveByOrderButton.setVisible(true);
            receiveByOrderButton.setManaged(true);
        }

        log.info("MovementController.initialize() завершён");
    }

    private void loadMovements() {
        movementsData.setAll(movementService.findAll());
        movementsTable.setItems(movementsData);
    }

    @FXML
    private void filterByDate() {
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            loadMovements();
            return;
        }
        LocalDateTime start = startDatePicker.getValue().atStartOfDay();
        LocalDateTime end = endDatePicker.getValue().atTime(23, 59, 59);
        movementsData.setAll(movementService.findByDateBetween(start, end));
        movementsTable.setItems(movementsData);
    }

    @FXML
    private void registerIncome() {
        openMovementDialog("Приход");
    }

    @FXML
    private void registerExpense() {
        openMovementDialog("Расход");
    }

    private void openMovementDialog(String type) {
        try {
            log.info("Открытие диалога движения, тип: {}", type);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/movementDialog.fxml"));
            loader.setControllerFactory(SpringFXMLLoader.getControllerFactory());
            Parent root = loader.load();

            MovementDialogController dialogController = loader.getController();
            dialogController.setMovementType(type);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(type.equals("Приход") ? "Приход товара" : "Расход товара");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(movementsTable.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

            // Обновляем таблицу движений после закрытия диалога
            loadMovements();

        } catch (IOException e) {
            log.error("Ошибка открытия диалога движения", e);
            showAlert("Ошибка", "Не удалось открыть окно регистрации: " + e.getMessage());
        }
    }

    @FXML
    private void receiveByOrder() {
        List<Order> availableOrders = orderService.findByStatus("Согласована");

        if (availableOrders.isEmpty()) {
            showAlert("Информация", "Нет согласованных заявок, готовых к приёму товара");
            return;
        }

        Dialog<Order> dialog = new Dialog<>();
        dialog.setTitle("Выбор заявки");
        dialog.setHeaderText("Выберите заявку, по которой поступает товар");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ListView<Order> listView = new ListView<>();
        listView.setItems(FXCollections.observableArrayList(availableOrders));

        listView.setCellFactory(param -> new ListCell<Order>() {
            @Override
            protected void updateItem(Order item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                }
            }
        });

        dialog.getDialogPane().setContent(listView);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return listView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        Optional<Order> result = dialog.showAndWait();
        result.ifPresent(order -> {
            openReceiveDialog(order);
        });
    }

    private void openReceiveDialog(Order order) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/receiveDialog.fxml"));
            loader.setControllerFactory(SpringFXMLLoader.getControllerFactory());
            Parent root = loader.load();

            ReceiveDialogController controller = loader.getController();
            controller.setOrder(order);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Поступление товара по заявке №" + order.getId());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(movementsTable.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

            loadMovements();

            refreshOrdersList();

        } catch (IOException e) {
            log.error("Ошибка открытия диалога поступления", e);
            showAlert("Ошибка", "Не удалось открыть окно поступления");
        }
    }

    private void refreshOrdersList() {
        try {
            // Находим OrderController и обновляем его таблицу
            for (Stage stage : Stage.getWindows().stream()
                    .filter(w -> w instanceof Stage)
                    .map(w -> (Stage) w)
                    .toList()) {
                Scene scene = stage.getScene();
                if (scene != null && scene.getRoot() != null) {
                    Object controller = findController(scene.getRoot());
                    if (controller instanceof OrderController) {
                        ((OrderController) controller).loadOrders();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка обновления списка заявок", e);
        }
    }

    private Object findController(Parent parent) {
        try {
            Object controller = parent.getProperties().get("javafx.fxml.Controller");
            if (controller != null) return controller;

            for (javafx.scene.Node node : parent.getChildrenUnmodifiable()) {
                if (node instanceof Parent) {
                    controller = findController((Parent) node);
                    if (controller != null) return controller;
                }
            }
        } catch (Exception e) {
            log.error("Ошибка поиска", e);
        }
        return null;
    }

    @FXML
    private void refreshMovements() {
        loadMovements();
        searchField.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}