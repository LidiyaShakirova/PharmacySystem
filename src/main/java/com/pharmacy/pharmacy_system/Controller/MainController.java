package com.pharmacy.pharmacy_system.Controller;

import com.pharmacy.pharmacy_system.Entity.User;
import com.pharmacy.pharmacy_system.Util.SpringFXMLLoader;
import com.pharmacy.pharmacy_system.Util.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MainController {

    @FXML
    private TabPane tabPane;

    @FXML
    private Label usernameLabel;

    private final SpringFXMLLoader springFXMLLoader;

    @FXML
    public void initialize() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            log.error("Пользователь не аутентифицирован");
            return;
        }
        usernameLabel.setText(currentUser.getUsername());

        String role = currentUser.getRole();

        // Очищаем все вкладки
        tabPane.getTabs().clear();

        // Создаём новую вкладку мониторинга (доступна всем)
        Tab newMonitorTab = new Tab("Мониторинг");
        newMonitorTab.setContent(loadFxml("/UI/monitor.fxml"));
        newMonitorTab.setClosable(false);
        tabPane.getTabs().add(newMonitorTab);

        if ("ADMIN".equals(role)) {
            Tab newAdminTab = new Tab(" Управление");
            newAdminTab.setContent(loadFxml("/UI/users.fxml"));
            newAdminTab.setClosable(false);
            tabPane.getTabs().add(newAdminTab);

        } else if ("PHARMACIST".equals(role)) {
            Tab newOrderTab = new Tab(" Заявки");
            newOrderTab.setContent(loadFxml("/UI/order.fxml"));
            newOrderTab.setClosable(false);
            tabPane.getTabs().add(newOrderTab);

            Tab newMovementTab = new Tab(" Движения");
            newMovementTab.setContent(loadFxml("/UI/movement.fxml"));
            newMovementTab.setClosable(false);
            tabPane.getTabs().add(newMovementTab);
        }


        tabPane.setStyle("-fx-font-size: 14px;");

        // ширина вкладок через CSS
        for (Tab tab : tabPane.getTabs()) {
            tab.setStyle("-fx-pref-width: 120px;");
        }
    }

    @FXML
    private void logout() {
        UserSession.getInstance().clearCurrentUser();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/login.fxml"));
            loader.setControllerFactory(springFXMLLoader.getControllerFactory());
            Parent root = loader.load();

            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Вход в систему");

            stage.setWidth(400);
            stage.setHeight(500);

            stage.centerOnScreen();

        } catch (IOException e) {
            log.error("Ошибка при выходе из системы", e);
            showAlert("Ошибка", "Не удалось загрузить окно входа");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Parent loadFxml(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            loader.setControllerFactory(springFXMLLoader.getControllerFactory());
            return loader.load();
        } catch (IOException e) {
            log.error("Ошибка загрузки FXML: " + path, e);
            throw new RuntimeException(e);
        }
    }
}