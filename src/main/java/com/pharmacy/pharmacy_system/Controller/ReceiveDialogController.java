package com.pharmacy.pharmacy_system.Controller;

import com.pharmacy.pharmacy_system.Entity.Order;
import com.pharmacy.pharmacy_system.Entity.OrderItem;
import com.pharmacy.pharmacy_system.Service.OrderService;
import javafx.beans.property.SimpleIntegerProperty;
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
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReceiveDialogController {

    @FXML
    private TableView<OrderItem> itemsTable;

    @FXML
    private TableColumn<OrderItem, String> drugColumn;

    @FXML
    private TableColumn<OrderItem, Integer> orderedColumn;

    @FXML
    private TableColumn<OrderItem, Integer> receivedColumn;

    @FXML
    private TableColumn<OrderItem, Integer> remainingColumn;

    @FXML
    private TableColumn<OrderItem, Integer> comingColumn;

    @FXML
    private Button cancelButton;

    @FXML
    private Label orderIdLabel;

    @FXML
    private Label statusLabel;

    private final OrderService orderService;
    private Order order;
    private ObservableList<OrderItem> items = FXCollections.observableArrayList();

    // Ссылка на родительский контроллер (окно деталей заявки)
    private OrderDetailsDialogController parentController;

    public void setParentController(OrderDetailsDialogController controller) {
        this.parentController = controller;
    }

    @FXML
    public void initialize() {
        log.info("ReceiveDialogController initialize");

        drugColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDrug().getName()));

        orderedColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        receivedColumn.setCellValueFactory(cellData -> {
            int received = cellData.getValue().getReceivedQuantity() != null ?
                    cellData.getValue().getReceivedQuantity() : 0;
            return new SimpleIntegerProperty(received).asObject();
        });

        remainingColumn.setCellValueFactory(cellData -> {
            int ordered = cellData.getValue().getQuantity();
            int received = cellData.getValue().getReceivedQuantity() != null ?
                    cellData.getValue().getReceivedQuantity() : 0;
            return new SimpleIntegerProperty(Math.max(ordered - received, 0)).asObject();
        });

        // Цветовая индикация строк
        itemsTable.setRowFactory(tv -> new TableRow<OrderItem>() {
            @Override
            protected void updateItem(OrderItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    int ordered = item.getQuantity();
                    int received = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0;
                    int coming = item.getComingQuantity() != null ? item.getComingQuantity() : 0;
                    int totalReceived = received + coming;
                    if (totalReceived >= ordered) {
                        setStyle("-fx-background-color: #c8e6c9;");
                    } else if (totalReceived > 0) {
                        setStyle("-fx-background-color: #fff9c4;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Колонка "Пришло" с полем ввода
        comingColumn.setCellFactory(col -> new TableCell<OrderItem, Integer>() {
            private final TextField textField = new TextField();
            {
                textField.setPrefWidth(80);
                textField.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && !newVal.matches("\\d*")) {
                        textField.setText(newVal.replaceAll("[^\\d]", ""));
                    }
                });
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                OrderItem orderItem = getTableRow().getItem();
                int alreadyReceived = orderItem.getReceivedQuantity() != null ?
                        orderItem.getReceivedQuantity() : 0;
                int ordered = orderItem.getQuantity();
                int remaining = ordered - alreadyReceived;
                int currentComing = orderItem.getComingQuantity() != null ?
                        orderItem.getComingQuantity() : 0;

                textField.setText(currentComing > 0 ? String.valueOf(currentComing) : "");
                textField.setPromptText("Макс: " + remaining);
                textField.setDisable(remaining == 0);

                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        String text = textField.getText().trim();
                        if (text.isEmpty()) {
                            orderItem.setComingQuantity(0);
                            updateRemainingInfo();
                            return;
                        }
                        try {
                            int value = Integer.parseInt(text);
                            if (value < 0) value = 0;
                            if (value > remaining) {
                                showWarning("Превышение", "Нельзя получить больше, чем заказано!\n" +
                                        "Заказано: " + ordered + ", уже получено: " + alreadyReceived +
                                        ", можно получить: " + remaining);
                                textField.setText("");
                                orderItem.setComingQuantity(0);
                            } else {
                                orderItem.setComingQuantity(value);
                                updateRemainingInfo();
                                itemsTable.refresh();
                            }
                        } catch (NumberFormatException e) {
                            orderItem.setComingQuantity(0);
                            textField.setText("");
                        }
                    }
                });

                setGraphic(textField);
                setText(null);
            }
        });

        itemsTable.setItems(items);
    }

    private void updateRemainingInfo() {
        int totalOrdered = 0;
        int totalReceived = 0;
        int totalComing = 0;
        int completedItems = 0;

        for (OrderItem item : items) {
            totalOrdered += item.getQuantity();
            int alreadyReceived = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0;
            int coming = item.getComingQuantity() != null ? item.getComingQuantity() : 0;
            totalReceived += alreadyReceived;
            totalComing += coming;
            if (alreadyReceived + coming >= item.getQuantity()) completedItems++;
        }

        int remaining = totalOrdered - (totalReceived + totalComing);
        if (remaining == 0) {
            statusLabel.setText("✓ Все препараты получены! Заявка будет закрыта.");
            statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else {
            statusLabel.setText("Осталось получить: " + remaining + " ед. (Выполнено: " + completedItems + "/" + items.size() + ")");
            statusLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void receiveAll() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Принять все препараты");
        confirm.setContentText("Вы уверены, что хотите принять ВСЕ оставшиеся препараты по этой заявке?\nЗаявка будет автоматически закрыта.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                for (OrderItem item : items) {
                    int alreadyReceived = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0;
                    int remaining = item.getQuantity() - alreadyReceived;
                    if (remaining > 0) {
                        item.setComingQuantity(remaining);
                    }
                }
                itemsTable.refresh();
                updateRemainingInfo();
                saveReceive();  // сразу сохранить
            }
        });
    }

    public void setOrder(Order order) {
        this.order = order;
        orderIdLabel.setText(String.valueOf(order.getId()));

        items.clear();
        items.addAll(order.getItems());

        for (OrderItem item : items) {
            if (item.getReceivedQuantity() == null) item.setReceivedQuantity(0);
            if (item.getComingQuantity() == null) item.setComingQuantity(0);
        }

        itemsTable.setItems(items);
        updateRemainingInfo();
        log.info("Загружена заявка №{} с {} позициями", order.getId(), items.size());
    }

    @FXML
    private void saveReceive() {
        List<OrderService.ReceiveItemDto> receivedItems = new ArrayList<>();
        boolean hasChanges = false;

        for (OrderItem item : items) {
            int coming = item.getComingQuantity() != null ? item.getComingQuantity() : 0;
            if (coming > 0) {
                OrderService.ReceiveItemDto dto = new OrderService.ReceiveItemDto();
                dto.setDrugId(item.getDrug().getId());
                dto.setReceivedQuantity(coming);
                receivedItems.add(dto);
                hasChanges = true;
            }
        }

        if (!hasChanges) {
            showAlert("Ошибка", "Введите поступившее количество хотя бы для одного препарата");
            return;
        }

        try {
            orderService.receiveOrder(order.getId(), receivedItems);
            // Обновляем родительское окно деталей заявки, если оно открыто
            if (parentController != null) {
                parentController.refreshOrder();
            }
            close();
        } catch (Exception e) {
            log.error("Ошибка сохранения", e);
            showAlert("Ошибка", e.getMessage());
        }
    }

    @FXML
    private void close() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}