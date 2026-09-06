package com.pds.pfe.ui.controllers;

import com.pds.pfe.ui.MainApp;
import com.pds.pfe.ui.services.ApiService;
import com.pds.pfe.ui.utils.SessionManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.util.Map;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    private Task<Map<String, Object>> currentTask;

    @FXML
    private void handleLogin() {
        if (currentTask != null && currentTask.isRunning()) {
            return;
        }

        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Connexion...");

        // Appel à la méthode login corrigée
        currentTask = ApiService.loginTask(username, password);

        currentTask.setOnSucceeded(event -> {
            loginButton.setDisable(false);
            loginButton.setText("Login");

            Map<String, Object> result = currentTask.getValue();

            if (result != null) {
                System.out.println("=== DEBUG: Réponse complète ===");
                System.out.println(result);

                // Extraire les données utilisateur
                Map<String, Object> data = (Map<String, Object>) result.get("data");

                if (data != null) {
                    Map<String, Object> userData = (Map<String, Object>) data.get("user");

                    if (userData != null) {
                        String userId = (String) userData.get("id");
                        String usernameFromServer = (String) userData.get("username");

                        System.out.println("=== DONNÉES EXTRAITES ===");
                        System.out.println("UserId: " + userId);
                        System.out.println("Username: " + usernameFromServer);

                        if (userId != null && usernameFromServer != null) {
                            // Connecter avec l'ID ET le username
                            SessionManager.login(usernameFromServer, userId);

                            // Vérification
                            System.out.println("=== VÉRIFICATION SESSION ===");
                            System.out.println("Session Username: " + SessionManager.getCurrentUser());
                            System.out.println("Session UserId: " + SessionManager.getCurrentUserId());

                            MainApp.showDashboardView();
                            return;
                        }
                    }
                }
            }

            // Si on arrive ici, il y a une erreur
            showAlert("Erreur", "Données de connexion invalides");
        });

        currentTask.setOnFailed(event -> {
            loginButton.setDisable(false);
            loginButton.setText("Login");
            showAlert("Erreur", "Erreur de connexion: " + currentTask.getException().getMessage());
        });

        // Exécuter la tâche
        Thread thread = new Thread(currentTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void showRegisterView() {
        MainApp.showRegisterView();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}