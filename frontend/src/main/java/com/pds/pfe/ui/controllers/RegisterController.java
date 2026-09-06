// src/main/java/com/pds/pfe/controllers/RegisterController.java
package com.pds.pfe.ui.controllers;

import com.pds.pfe.ui.MainApp;
import com.pds.pfe.ui.services.ApiService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    private Task<Boolean> currentTask;

    @FXML
    private void handleBack() {
        // Annuler la tâche en cours si elle existe
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }
        MainApp.showLoginView();
    }
    @FXML
    private void handleRegister() {
        if (currentTask != null && currentTask.isRunning()) {
            return;
        }

        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (!validateInput(fullName, email, password, confirmPassword)) {
            return;
        }

        registerButton.setDisable(true);
        registerButton.setText("Inscription...");

        // Appel direct avec les strings
        currentTask = ApiService.registerTask(fullName, email, password);

        currentTask.setOnSucceeded(event -> {
            registerButton.setDisable(false);
            registerButton.setText("s'inscrire");

            Boolean success = currentTask.getValue();
            if (success) {
                showSuccessAlert("Inscription réussie !", "Vous pouvez maintenant vous connecter.");
                MainApp.showLoginView();
            } else {
                showAlert("Erreur", "Erreur lors de l'inscription. L'email existe peut-être déjà.");
            }
        });

        currentTask.setOnFailed(event -> {
            registerButton.setDisable(false);
            registerButton.setText("s'inscrire");
            showAlert("Erreur", "Erreur de connexion: " + currentTask.getException().getMessage());
        });
    }

    private boolean validateInput(String fullName, String email, String password, String confirmPassword) {
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            showAlert("Erreur", "Les mots de passe ne correspondent pas");
            passwordField.clear();
            confirmPasswordField.clear();
            passwordField.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            showAlert("Erreur", "Le mot de passe doit contenir au moins 6 caractères");
            passwordField.clear();
            confirmPasswordField.clear();
            passwordField.requestFocus();
            return false;
        }

        if (!isValidEmail(email)) {
            showAlert("Erreur", "Veuillez entrer une adresse email valide");
            emailField.requestFocus();
            return false;
        }

        if (fullName.length() < 2) {
            showAlert("Erreur", "Le nom complet doit contenir au moins 2 caractères");
            fullNameField.requestFocus();
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void cleanup() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }
    }
}