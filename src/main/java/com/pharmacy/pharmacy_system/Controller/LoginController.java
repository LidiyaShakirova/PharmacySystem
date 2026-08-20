package com.pharmacy.pharmacy_system.Controller;

import com.pharmacy.pharmacy_system.Entity.User;
import com.pharmacy.pharmacy_system.Service.UserService;
import com.pharmacy.pharmacy_system.Util.SpringFXMLLoader;
import com.pharmacy.pharmacy_system.Util.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.Optional;

import static org.hibernate.query.sqm.tree.SqmNode.log;


@Controller
@RequiredArgsConstructor

public class LoginController {
    private final UserService userService;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;



    @FXML
    private CheckBox showPasswordCheckBox;

    @FXML
    private Label errorLabel;
    @FXML
    private TextField visiblePasswordField;

    @FXML
    public void initialize() {
        // Синхронизируем текст между полями пароля
        passwordField.textProperty().bindBidirectional(visiblePasswordField.textProperty());

        // Переключение видимости пароля
        showPasswordCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                passwordField.setVisible(false);
                passwordField.setManaged(false);
                visiblePasswordField.setVisible(true);
                visiblePasswordField.setManaged(true);
                visiblePasswordField.requestFocus();
            } else {
                visiblePasswordField.setVisible(false);
                visiblePasswordField.setManaged(false);
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                passwordField.requestFocus();
            }
        });

        // По умолчанию скрыто
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText(); // или visiblePasswordField.getText() – они одинаковы

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Заполните все поля");
            return;
        }

        Optional<User> userOpt = userService.authenticate(username, password);

        if (userOpt.isPresent()) {
            UserSession.getInstance().setCurrentUser(userOpt.get());
            openMainWindow();
        } else {
            errorLabel.setText("Неверное имя пользователя или пароль");
        }
    }


    private void openMainWindow() {
        try {
            // Загружаем main.fxml с использованием Spring-фабрики контроллеров
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/main.fxml"));
            loader.setControllerFactory(SpringFXMLLoader.getControllerFactory());
            Parent root = loader.load();

            // Получаем текущее окно (Stage) из любого элемента интерфейса
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Главное меню");

            stage.setWidth(1000);
            stage.setHeight(700);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Ошибка загрузки главного окна");
        }
    }
}


