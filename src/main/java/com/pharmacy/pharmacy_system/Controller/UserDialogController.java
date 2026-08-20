package com.pharmacy.pharmacy_system.Controller;

import com.pharmacy.pharmacy_system.Entity.User;
import com.pharmacy.pharmacy_system.Service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDialogController {

    private final UserService userService;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Button saveButton;

    private User user;

    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("ADMIN", "PHARMACIST");
        roleComboBox.setValue("PHARMACIST");
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            usernameField.setText(user.getUsername());
            passwordField.setPromptText("Оставьте пустым, если не меняете");
            roleComboBox.setValue(user.getRole());
        }
    }

    @FXML
    private void saveUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        if (username.isEmpty()) {
            showAlert("Ошибка", "Имя пользователя не может быть пустым");
            return;
        }
        if (role == null) {
            showAlert("Ошибка", "Выберите роль");
            return;
        }

        try {
            if (user == null) {
                // Новый пользователь: пароль обязателен
                if (password.isEmpty()) {
                    showAlert("Ошибка", "Пароль не может быть пустым для нового пользователя");
                    return;
                }
                userService.createUser(username, password, role);
            } else {
                // Обновление существующего: если пароль не введён, оставляем старый
                userService.updateUser(user.getId(), username, role, password.isEmpty() ? null : password);
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