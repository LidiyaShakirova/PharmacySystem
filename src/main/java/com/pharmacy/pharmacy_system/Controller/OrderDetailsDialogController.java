package com.pharmacy.pharmacy_system.Controller;


import com.pharmacy.pharmacy_system.Entity.Order;
import com.pharmacy.pharmacy_system.Entity.OrderItem;
import com.pharmacy.pharmacy_system.Entity.User;
import com.pharmacy.pharmacy_system.Service.OrderItemService;
import com.pharmacy.pharmacy_system.Service.OrderService;
import com.pharmacy.pharmacy_system.Util.SpringFXMLLoader;
import com.pharmacy.pharmacy_system.Util.UserSession;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDetailsDialogController {

    private final OrderItemService orderItemService;
    private final OrderService orderService;

    @FXML
    private Label idLabel;
    @FXML
    private Label userLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private TableView<OrderItem> itemsTable;
    @FXML
    private TableColumn<OrderItem, String> drugNameColumn;
    @FXML
    private TableColumn<OrderItem, Integer> quantityColumn;
    @FXML
    private TableColumn<OrderItem, Integer> receivedColumn;
    @FXML
    private TableColumn<OrderItem, Integer> remainingColumn;
    @FXML
    private Button closeButton;
    @FXML
    private Button exportButton;
    @FXML
    private Button editButton;
    @FXML
    private Button changeStatusButton;

    private Order order;

    @FXML
    public void initialize() {

        drugNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDrug().getName()));

        quantityColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getQuantity()).asObject());

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
                    if (received >= ordered) {
                        setStyle("-fx-background-color: #c8e6c9;");
                    } else if (received > 0) {
                        setStyle("-fx-background-color: #fff9c4;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    public void setOrder(Order order) {
        this.order = order;
        if (order != null) {
            idLabel.setText("№ " + order.getId());
            userLabel.setText(order.getUser().getUsername());
            statusLabel.setText(order.getStatus());
            dateLabel.setText(order.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));


            User currentUser = UserSession.getInstance().getCurrentUser();
            if ("PHARMACIST".equals(currentUser.getRole()) && "Черновик".equals(order.getStatus())
                    && order.getUser().getId().equals(currentUser.getId())) {
                editButton.setVisible(true);
                changeStatusButton.setVisible(true);
            } else if ("PHARMACIST".equals(currentUser.getRole()) && "Согласована".equals(order.getStatus())
                    && order.getUser().getId().equals(currentUser.getId())) {
                changeStatusButton.setVisible(true);
            }

            // Получаем позиции заявки
            List<OrderItem> items = orderItemService.findByOrderId(order.getId());

            // СОРТИРОВКА: сначала неполученные, потом полученные; внутри – по приоритету и остатку
            items.sort(Comparator
                    .comparingInt((OrderItem item) -> {
                        int received = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0;
                        int remaining = item.getQuantity() - received;
                        return remaining > 0 ? 0 : 1;  // неполученные (осталось > 0) вперёд
                    })
                    .thenComparingInt(item -> item.getDrug().getCategory().getPriority())
                    .thenComparingInt(item -> {
                        int received = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0;
                        return -(item.getQuantity() - received);
                    })
            );

            itemsTable.setItems(FXCollections.observableArrayList(items));
        }
    }
    public void refreshOrder() {
        if (order != null) {
            setOrder(orderService.findById(order.getId()));
        }
    }

    @FXML
    private void editOrder() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/orderDialog.fxml"));
            loader.setControllerFactory(SpringFXMLLoader.getControllerFactory());
            Parent root = loader.load();

            OrderDialogController controller = loader.getController();
            controller.setOrder(order);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Редактирование заявки №" + order.getId());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(editButton.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

            refreshOrder();  // обновить после редактирования

        } catch (IOException e) {
            showAlert("Ошибка", "Не удалось открыть окно редактирования");
        }
    }

    @FXML
    private void changeStatus() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(order.getStatus(),
                "Черновик", "Согласована", "Закрыта");
        dialog.setTitle("Изменение статуса");
        dialog.setHeaderText("Заявка №" + order.getId());
        dialog.setContentText("Новый статус:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newStatus -> {
            if (!newStatus.equals(order.getStatus())) {
                try {
                    orderService.updateStatus(order.getId(), newStatus);
                    showInfo("Успех", "Статус заявки №" + order.getId() + " изменён на '" + newStatus + "'");
                    refreshOrder();
                } catch (Exception e) {
                    showAlert("Ошибка", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void exportToExcel() {
        if (order == null) {
            showAlert("Ошибка", "Заявка не выбрана");
            return;
        }

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Сохранить заявку");
            fileChooser.setInitialFileName("Заявка_№" + order.getId() + ".xlsx");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel files", "*.xlsx")
            );

            File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());

            if (file != null) {
                exportOrderToExcel(order, file);
                showInfo("Успех", "Заявка выгружена в файл:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Ошибка выгрузки", e);
            showAlert("Ошибка", "Не удалось выгрузить заявку: " + e.getMessage());
        }
    }

    private void exportOrderToExcel(Order order, File file) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Заявка №" + order.getId());

        // Стили
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);

        // Шапка заявки
        Row titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue("ЗАЯВКА № " + order.getId());

        Row dateRow = sheet.createRow(1);
        dateRow.createCell(0).setCellValue("Дата: " + order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        dateRow.createCell(1).setCellValue("Статус: " + order.getStatus());

        Row userRow = sheet.createRow(2);
        userRow.createCell(0).setCellValue("Фармацевт: " + order.getUser().getUsername());

        sheet.createRow(3);

        // Заголовки таблицы
        Row headerRow = sheet.createRow(4);
        String[] columns = {"№", "Наименование препарата", "Категория", "Приоритет", "Заказано", "Поступило", "Осталось"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        // Получаем позиции заявки и применяем ту же сортировку, что и в таблице
        List<OrderItem> items = orderItemService.findByOrderId(order.getId());
        items.sort(Comparator
                .comparingInt((OrderItem item) -> {
                    int received = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0;
                    int remaining = item.getQuantity() - received;
                    return remaining > 0 ? 0 : 1;  // неполученные вперёд
                })
                .thenComparingInt(item -> item.getDrug().getCategory().getPriority())
                .thenComparingInt(item -> {
                    int received = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0;
                    return -(item.getQuantity() - received);
                })
        );

        int rowNum = 5;
        int num = 1;
        for (OrderItem item : items) {
            Row row = sheet.createRow(rowNum);
            row.createCell(0).setCellValue(num++);
            row.createCell(1).setCellValue(item.getDrug().getName());
            row.createCell(2).setCellValue(item.getDrug().getCategory().getName());
            row.createCell(3).setCellValue(item.getDrug().getCategory().getPriority());
            row.createCell(4).setCellValue(item.getQuantity());

            int received = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0;
            row.createCell(5).setCellValue(received);
            row.createCell(6).setCellValue(item.getQuantity() - received);

            for (int i = 0; i < columns.length; i++) {
                if (row.getCell(i) != null) {
                    row.getCell(i).setCellStyle(cellStyle);
                }
            }
            rowNum++;
        }

        // Итоговая строка
        Row totalRow = sheet.createRow(rowNum + 1);
        totalRow.createCell(4).setCellValue("Итого:");
        totalRow.createCell(5).setCellValue(items.stream().mapToInt(OrderItem::getQuantity).sum());

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            workbook.write(outputStream);
        }
        workbook.close();
    }

    @FXML
    private void close() {
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
}