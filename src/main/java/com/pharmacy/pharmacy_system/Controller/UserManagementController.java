package com.pharmacy.pharmacy_system.Controller;

import com.pharmacy.pharmacy_system.Entity.User;
import com.pharmacy.pharmacy_system.Service.UserService;
import com.pharmacy.pharmacy_system.Util.SpringFXMLLoader;
import com.pharmacy.pharmacy_system.Util.UserSession;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserManagementController {

    private final UserService userService;

    @FXML
    private TableView<User> usersTable;
    @FXML private TableColumn<User, Long> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> createdAtColumn;

    private ObservableList<User> usersData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        createdAtColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));

        loadUsers();
    }

    private void loadUsers() {
        usersData.setAll(userService.findAll());
        usersTable.setItems(usersData);
    }

    @FXML
    private void addUser() {
        openUserDialog(null);
    }

    @FXML
    private void editUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите пользователя");
            return;
        }
        openUserDialog(selected);
    }

    @FXML
    private void deleteUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите пользователя");
            return;
        }
        if (selected.getId().equals(UserSession.getInstance().getCurrentUser().getId())) {
            showAlert("Ошибка", "Нельзя удалить самого себя");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Удалить пользователя " + selected.getUsername() + "?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                userService.deleteUser(selected.getId());
                loadUsers();
            }
        });
    }

    private void openUserDialog(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/userDialog.fxml"));
            loader.setControllerFactory(SpringFXMLLoader.getControllerFactory());
            Parent root = loader.load();

            UserDialogController dialogController = loader.getController();
            dialogController.setUser(user);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(user == null ? "Новый пользователь" : "Редактирование пользователя");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(usersTable.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

            loadUsers(); // обновить после закрытия
        } catch (IOException e) {
            log.error("Ошибка открытия диалога пользователя", e);
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