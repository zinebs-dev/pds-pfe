// src/main/java/com/pds/pfe/MainApp.java
package com.pds.pfe.ui;

import com.pds.pfe.ui.utils.TaskExecutor;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;

public class MainApp extends Application {

    private static Stage primaryStage;
    private static double windowX = -1;
    private static double windowY = -1;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        primaryStage.setOnCloseRequest(event -> {
            TaskExecutor.shutdown();
        });

        showLoginView();
    }

    public static void showLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(MainApp.class.getResource("/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            Image icon = new Image(Objects.requireNonNull(
                    MainApp.class.getResourceAsStream("/images/logo1.png")
            ));
            primaryStage.getIcons().add(icon);
            primaryStage.setTitle("PDS-PFE - Login");
            primaryStage.setResizable(false);

            // Centrer la fenêtre si c'est la première fois
            if (windowX == -1 || windowY == -1) {
                primaryStage.centerOnScreen();
            } else {
                primaryStage.setX(windowX);
                primaryStage.setY(windowY);
            }

            primaryStage.show();

            // Sauvegarder la position
            windowX = primaryStage.getX();
            windowY = primaryStage.getY();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showRegisterView() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/register.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(MainApp.class.getResource("/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("PDS-PFE - Inscription");
            primaryStage.setResizable(false);

            // Maintenir la position
            if (windowX != -1 && windowY != -1) {
                primaryStage.setX(windowX);
                primaryStage.setY(windowY);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showDashboardView() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/upload.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(MainApp.class.getResource("/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("PDS-PFE - Dashboard");
            primaryStage.setResizable(true);

            // Maintenir la position
            if (windowX != -1 && windowY != -1) {
                primaryStage.setX(windowX);
                primaryStage.setY(windowY);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void stop() throws Exception {
        TaskExecutor.shutdown();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}