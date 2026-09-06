package com.pds.pfe.ui.controllers;

import com.pds.pfe.ui.MainApp;
import com.pds.pfe.ui.services.ApiService;
import com.pds.pfe.ui.utils.SessionManager;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.util.*;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Button importButton;
    @FXML private Button sendButton;
    @FXML private Label fileLabel;
    @FXML private VBox resultsContainer;
    @FXML private Label similarityResult;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private VBox historyContainer;
    @FXML private Label emptyHistoryLabel;
    @FXML private Button darkModeButton;
    @FXML private Button toggleSidebarButton;
    @FXML private Button toggleSidebarButtonOutside;
    @FXML private VBox sidebarContainer;
    @FXML private Label welcomeMessageLabel;

    private File selectedFile;
    private Task<Map<String, Object>> currentTask;
    private boolean isDarkMode = false;
    private boolean isSidebarVisible = true;

    @FXML
    public void initialize() {
        // Log pour debug
        System.out.println("Dashboard initialisé pour l'utilisateur:");
        System.out.println("  Username: " + SessionManager.getCurrentUser());
        System.out.println("  User ID: " + SessionManager.getCurrentUserId());

        // Afficher le message de bienvenue avec le nom d'utilisateur
        if (welcomeMessageLabel != null) {
            String username = SessionManager.getCurrentUser();
            if (username != null && !username.isEmpty()) {
                welcomeMessageLabel.setText("👋 Bienvenue, " + username);
            } else {
                welcomeMessageLabel.setText("👋 Bienvenue");
            }
        }

        // S'assurer que les éléments sont dans le bon état initial
        if (resultsContainer != null) {
            resultsContainer.setVisible(false);
            resultsContainer.setManaged(false);
        }

        if (progressBar != null) {
            progressBar.setVisible(false);
        }

        if (progressLabel != null) {
            progressLabel.setVisible(false);
        }

        if (fileLabel != null) {
            fileLabel.setVisible(false);
        }

        if (sendButton != null) {
            sendButton.setDisable(true);
        }

        // Charger l'historique de l'utilisateur
        loadUserHistory();
        setupCloseConfirmation();
    }

    private void loadUserHistory() {
        String userId = SessionManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            System.out.println("Aucun user ID trouvé pour charger l'historique");
            return;
        }

        Task<Map<String, Object>> historyTask = ApiService.getHistoryTask(userId);

        historyTask.setOnSucceeded(event -> {
            Map<String, Object> result = historyTask.getValue();
            if (result != null && result.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                List<Map<String, Object>> history = (List<Map<String, Object>>) data.get("history");
                displayHistory(history);
            }
        });

        historyTask.setOnFailed(event -> {
            System.err.println("Erreur lors du chargement de l'historique: " + historyTask.getException().getMessage());
        });

        // Exécutez la tâche
        Thread historyThread = new Thread(historyTask);
        historyThread.setDaemon(true);
        historyThread.start();
    }

    @SuppressWarnings("unchecked")
    private void displayHistory(List<Map<String, Object>> history) {
        if (historyContainer == null) {
            return;
        }

        historyContainer.getChildren().clear();

        if (history == null || history.isEmpty()) {
            if (emptyHistoryLabel != null) {
                emptyHistoryLabel.setVisible(true);
                historyContainer.getChildren().add(emptyHistoryLabel);
            }
            return;
        }

        if (emptyHistoryLabel != null) {
            emptyHistoryLabel.setVisible(false);
        }

        for (Map<String, Object> item : history) {
            VBox historyItem = createHistoryItem(item);
            historyContainer.getChildren().add(historyItem);
        }
    }

    private void loadHistoryResult(String resultId) {
        if (resultId == null || resultId.isEmpty()) {
            showAlert("Erreur", "ID de résultat invalide");
            return;
        }

        System.out.println("Chargement du résultat historique: " + resultId);

        // Afficher un indicateur de chargement
        if (progressBar != null) {
            progressBar.setVisible(true);
            progressBar.setProgress(-1); // Indéterminé
        }
        if (progressLabel != null) {
            progressLabel.setVisible(true);
            progressLabel.setText("Chargement du résultat...");
        }

        Task<Map<String, Object>> loadTask = ApiService.getHistoryResultTask(resultId);

        loadTask.setOnSucceeded(event -> {
            Map<String, Object> result = loadTask.getValue();
            if (result != null) {
                // Afficher les résultats
                displayHistoryResult(result);
            } else {
                showAlert("Erreur", "Impossible de charger les détails du résultat");
            }

            // Masquer l'indicateur de chargement
            if (progressBar != null) {
                progressBar.setVisible(false);
            }
            if (progressLabel != null) {
                progressLabel.setVisible(false);
            }
        });

        loadTask.setOnFailed(event -> {
            showAlert("Erreur", "Erreur lors du chargement: " + loadTask.getException().getMessage());

            if (progressBar != null) {
                progressBar.setVisible(false);
            }
            if (progressLabel != null) {
                progressLabel.setVisible(false);
            }
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    @SuppressWarnings("unchecked")
    private void displayHistoryResult(Map<String, Object> historyResult) {
        try {
            System.out.println("=== Affichage résultat historique ===");
            Map<String, Object> historyData = (Map<String, Object>) historyResult.get("data");

            if (historyData == null) {
                showAlert("Erreur", "Données historiques manquantes");
                return;
            }

            // Extraire les données DEPUIS historyData
            double overallSimilarity = 0.0;
            Object simObj = historyData.get("overall_similarity");
            if (simObj instanceof Number) {
                overallSimilarity = ((Number) simObj).doubleValue();
            }

            Map<String, Object> sectionSimilarities = (Map<String, Object>) historyData.get("section_similarities");
            List<Map<String, Object>> topMatches = (List<Map<String, Object>>) historyData.get("top_matches");
            String filename = (String) historyData.get("filename");

            // Nettoyer le conteneur
            if (resultsContainer != null) {
                resultsContainer.getChildren().clear();
            }

            // === TITRE DU RAPPORT ===
            Label reportTitle = new Label("📊 Rapport d'Analyse de Similarité");
            reportTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + getTitleColor() + "; -fx-padding: 0 0 10 0;");
            reportTitle.setMaxWidth(Double.MAX_VALUE);

            Label fileLabel = new Label("Fichier: " + (filename != null ? filename : "N/A"));
            fileLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + getSubtitleColor() + "; -fx-padding: 0 0 20 0;");
            fileLabel.setMaxWidth(Double.MAX_VALUE);

            resultsContainer.getChildren().addAll(reportTitle, fileLabel);

            // === SECTION 1: TAUX DE SIMILARITÉ GLOBAL ===
            VBox globalSection = createGlobalSimilaritySection(overallSimilarity);
            resultsContainer.getChildren().add(globalSection);

            // === SECTION 2 : NIVEAU GLOBAL DE RISQUE ===
            HBox riskSection = createGlobalRiskSection(overallSimilarity);
            resultsContainer.getChildren().add(riskSection);

            // === SECTION 3 : DÉTAILS PAR SECTION ===
            if (sectionSimilarities != null && !sectionSimilarities.isEmpty()) {
                VBox sectionsTable = createSectionsTable(sectionSimilarities);
                resultsContainer.getChildren().add(sectionsTable);
            }

            // === SECTION 4: TOP DOCUMENTS SIMILAIRES ===
            if (topMatches != null && !topMatches.isEmpty()) {
                VBox similarDocsSection = createSimilarDocumentsSection(topMatches);
                resultsContainer.getChildren().add(similarDocsSection);
            } else {
                VBox noMatchesBox = new VBox(10);
                noMatchesBox.setStyle("-fx-background-color: " + getSectionBgColor() + "; -fx-background-radius: 12px; -fx-padding: 25px;");
                noMatchesBox.setMaxWidth(Double.MAX_VALUE);

                Label noMatchesTitle = new Label("📄 Top Documents Similaires");
                noMatchesTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e8ecf1;");

                Label noMatchesMsg = new Label("Les détails des documents similaires ne sont pas disponibles pour cet historique.");
                noMatchesMsg.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e0;");
                noMatchesMsg.setWrapText(true);

                noMatchesBox.getChildren().addAll(noMatchesTitle, noMatchesMsg);
                resultsContainer.getChildren().add(noMatchesBox);
            }

            // === SECTION 5: RECOMMANDATIONS ===
            VBox recommendations = createRecommendationsSection(overallSimilarity, sectionSimilarities);
            resultsContainer.getChildren().add(recommendations);

            // Afficher le conteneur
            if (resultsContainer != null) {
                resultsContainer.setVisible(true);
                resultsContainer.setManaged(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de l'affichage: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private VBox createHistoryItem(Map<String, Object> item) {
        VBox itemBox = new VBox(8);
        itemBox.setPadding(new Insets(12));

        // Couleurs adaptatives selon le mode
        String bgColor = isDarkMode ? "rgba(30, 41, 54, 0.6)" : "rgba(88, 101, 242, 0.08)";
        String borderColor = isDarkMode ? "rgba(88, 101, 242, 0.3)" : "rgba(88, 101, 242, 0.2)";

        itemBox.setStyle(
                "-fx-background-color: " + bgColor + "; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: " + borderColor + "; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 5, 0, 0, 2);"
        );

        String filename = (String) item.get("filename");
        Object similarityObj = item.get("overall_similarity");
        String comparisonDate = (String) item.get("comparison_date");
        String resultId = (String) item.get("id");

        // ===== HEADER AVEC BOUTON SUPPRESSION =====
        HBox headerBox = new HBox();
        headerBox.setSpacing(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label filenameLabel = new Label("📄 " + filename);
        String filenameLabelColor = isDarkMode ? "#e8ecf1" : "#1a1a2e";
        filenameLabel.setStyle(
                "-fx-font-weight: bold; " +
                        "-fx-font-size: 13px; " +
                        "-fx-text-fill: " + filenameLabelColor + ";"
        );

        // Espace pour pousser le bouton à droite
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // BOUTON DE SUPPRESSION
        Button deleteButton = new Button("🗑");
        deleteButton.getStyleClass().add("history-delete-button");
        deleteButton.setTooltip(new Tooltip("Supprimer cet élément de l'historique"));

        deleteButton.setOnAction(event -> {
            confirmAndDeleteHistoryItem(resultId, filename, itemBox);
        });

        headerBox.getChildren().addAll(filenameLabel, spacer, deleteButton);

        double similarity = 0.0;
        if (similarityObj instanceof Number) {
            similarity = ((Number) similarityObj).doubleValue();
        }

        // Badge de similarité
        HBox similarityBox = new HBox(8);
        similarityBox.setAlignment(Pos.CENTER_LEFT);

        Circle indicator = new Circle(5);
        String indicatorColor = getAlertColor(similarity);
        indicator.setFill(Color.web(indicatorColor));

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(indicatorColor));
        glow.setRadius(8);
        glow.setSpread(0.4);
        indicator.setEffect(glow);

        Label similarityLabel = new Label(String.format("%.1f%%", similarity));
        similarityLabel.setStyle(
                "-fx-font-size: 13px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: " + indicatorColor + ";"
        );

        Label similarityText = new Label("Similarité");
        String textColor = isDarkMode ? "#9ca3af" : "#6b7280";
        similarityText.setStyle("-fx-font-size: 11px; -fx-text-fill: " + textColor + ";");

        similarityBox.getChildren().addAll(indicator, similarityLabel, similarityText);

        String formattedDate = "N/A";
        if (comparisonDate != null && comparisonDate.length() >= 16) {
            formattedDate = comparisonDate.substring(0, 16).replace("T", " ");
        } else if (comparisonDate != null && comparisonDate.length() >= 10) {
            formattedDate = comparisonDate.substring(0, 10);
        }

        Label dateLabel = new Label("📅 " + formattedDate);
        String dateColor = isDarkMode ? "#9ca3af" : "#6b7280";
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + dateColor + ";");

        itemBox.getChildren().addAll(headerBox, similarityBox, dateLabel);

        itemBox.setOnMouseClicked(event -> loadHistoryResult(resultId));

        // Effet hover
        String hoverBg = isDarkMode ? "rgba(88, 101, 242, 0.15)" : "rgba(88, 101, 242, 0.12)";
        String hoverBorder = isDarkMode ? "rgba(88, 101, 242, 0.5)" : "rgba(88, 101, 242, 0.4)";

        itemBox.setOnMouseEntered(event -> {
            itemBox.setStyle(
                    "-fx-background-color: " + hoverBg + "; " +
                            "-fx-background-radius: 8px; " +
                            "-fx-border-color: " + hoverBorder + "; " +
                            "-fx-border-width: 1.5px; " +
                            "-fx-border-radius: 8px; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(88, 101, 242, 0.3), 8, 0, 0, 3);"
            );
        });

        itemBox.setOnMouseExited(event -> {
            itemBox.setStyle(
                    "-fx-background-color: " + bgColor + "; " +
                            "-fx-background-radius: 8px; " +
                            "-fx-border-color: " + borderColor + "; " +
                            "-fx-border-width: 1px; " +
                            "-fx-border-radius: 8px; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 5, 0, 0, 2);"
            );
        });

        return itemBox;
    }

    private void confirmAndDeleteHistoryItem(String resultId, String filename, VBox itemBox) {
        // --- MISE À JOUR : Utilisation du système d'alerte moderne ---
        boolean confirmed = showModernConfirmation(
                "Suppression",
                "Êtes-vous sûr de vouloir supprimer '" + filename + "' de votre historique ?",
                "Supprimer",
                "Annuler"
        );

        if (confirmed) {
            itemBox.setStyle("-fx-background-color: #fadbd8; -fx-opacity: 0.7; -fx-background-radius: 5;");
            itemBox.setDisable(true);

            Task<Boolean> deleteTask = ApiService.deleteHistoryItemTask(resultId);

            deleteTask.setOnSucceeded(event -> {
                if (deleteTask.getValue()) {
                    if (historyContainer != null) {
                        historyContainer.getChildren().remove(itemBox);
                    }
                    showModernAlert("Succès", "L'élément a été supprimé de l'historique.", Alert.AlertType.INFORMATION);

                    if (historyContainer.getChildren().isEmpty() && emptyHistoryLabel != null) {
                        emptyHistoryLabel.setVisible(true);
                        historyContainer.getChildren().add(emptyHistoryLabel);
                    }
                } else {
                    showModernAlert("Erreur", "Échec de la suppression. Veuillez réessayer.", Alert.AlertType.ERROR);
                    itemBox.setStyle("-fx-background-color: #f3f4f6; -fx-opacity: 1.0; -fx-background-radius: 5;");
                    itemBox.setDisable(false);
                }
            });

            deleteTask.setOnFailed(event -> {
                showModernAlert("Erreur", "Erreur lors de la suppression: " + deleteTask.getException().getMessage(), Alert.AlertType.ERROR);
                itemBox.setStyle("-fx-background-color: #f3f4f6; -fx-opacity: 1.0; -fx-background-radius: 5;");
                itemBox.setDisable(false);
            });

            Thread deleteThread = new Thread(deleteTask);
            deleteThread.setDaemon(true);
            deleteThread.start();
        }
    }

    @FXML
    private void handleImportFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier PDF");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        selectedFile = fileChooser.showOpenDialog(MainApp.getPrimaryStage());

        if (selectedFile != null) {
            if (!selectedFile.getName().toLowerCase().endsWith(".pdf")) {
                showAlert("Erreur", "Veuillez sélectionner un fichier PDF");
                selectedFile = null;
                sendButton.setDisable(true);
                if (fileLabel != null) {
                    fileLabel.setVisible(false);
                }
            } else {
                if (fileLabel != null) {
                    fileLabel.setText("📄 " + selectedFile.getName());
                    fileLabel.setVisible(true);
                }
                if (sendButton != null) {
                    sendButton.setDisable(false);
                }
                if (resultsContainer != null) {
                    resultsContainer.setVisible(false);
                    resultsContainer.setManaged(false);
                }
            }
        }
    }

    @FXML
    private void handleSend() {
        if (selectedFile == null) {
            showAlert("Erreur", "Veuillez d'abord importer un fichier PDF");
            return;
        }

        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }

        sendButton.setDisable(true);
        importButton.setDisable(true);
        progressBar.setVisible(true);
        progressLabel.setVisible(true);
        progressBar.setProgress(0);

        if (resultsContainer != null) {
            resultsContainer.setVisible(false);
            resultsContainer.setManaged(false);
        }

        currentTask = ApiService.uploadAndAnalyzeTask(selectedFile);

        progressBar.progressProperty().bind(currentTask.progressProperty());
        progressLabel.textProperty().bind(currentTask.messageProperty());

        currentTask.setOnSucceeded(event -> {
            cleanupTask();
            Map<String, Object> result = currentTask.getValue();
            if (result != null) {
                displayResults(result);
                loadUserHistory();
            } else {
                showAlert("Erreur", "Aucun résultat retourné par l'analyse");
            }
            sendButton.setDisable(false);
            importButton.setDisable(false);
        });

        currentTask.setOnFailed(event -> {
            cleanupTask();
            showAlert("Erreur", "Échec du traitement: " + currentTask.getException().getMessage());
            sendButton.setDisable(false);
            importButton.setDisable(false);
        });

        currentTask.setOnCancelled(event -> {
            cleanupTask();
            sendButton.setDisable(false);
            importButton.setDisable(false);
        });

        Thread thread = new Thread(currentTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void setupCloseConfirmation() {
        javafx.application.Platform.runLater(() -> {
            Stage stage = MainApp.getPrimaryStage();
            if (stage != null) {
                stage.setOnCloseRequest(event -> {
                    event.consume();
                    boolean shouldClose = showExitConfirmation();
                    if (shouldClose) {
                        if (currentTask != null && currentTask.isRunning()) {
                            currentTask.cancel();
                        }
                        stage.close();
                        System.exit(0);
                    }
                });
            }
        });
    }

    private boolean showExitConfirmation() {
        // --- MISE À JOUR : Utilisation du système d'alerte moderne ---
        return showModernConfirmation(
                "Fermeture",
                "Voulez-vous vraiment quitter l'application ? Toute analyse en cours sera annulée.",
                "Quitter",
                "Rester"
        );
    }

    @FXML
    private void handleLogout() {
        // --- MISE À JOUR : Utilisation du système d'alerte moderne ---
        boolean confirmed = showModernConfirmation(
                "Déconnexion",
                "Êtes-vous sûr de vouloir vous déconnecter ?",
                "Se déconnecter",
                "Annuler"
        );

        if (confirmed) {
            if (currentTask != null && currentTask.isRunning()) {
                currentTask.cancel();
            }
            SessionManager.logout();
            MainApp.showLoginView();
        }
    }

    @FXML
    private void handleToggleSidebar() {
        isSidebarVisible = !isSidebarVisible;

        if (sidebarContainer != null) {
            TranslateTransition transition = new TranslateTransition(Duration.millis(300), sidebarContainer);

            if (isSidebarVisible) {
                sidebarContainer.setVisible(true);
                sidebarContainer.setManaged(true);
                transition.setFromX(-sidebarContainer.getWidth());
                transition.setToX(0);

                if (toggleSidebarButton != null) {
                    toggleSidebarButton.setVisible(true);
                    toggleSidebarButton.setManaged(true);
                    toggleSidebarButton.setText("☰");
                }
                if (toggleSidebarButtonOutside != null) {
                    toggleSidebarButtonOutside.setVisible(false);
                    toggleSidebarButtonOutside.setManaged(false);
                }
            } else {
                transition.setFromX(0);
                transition.setToX(-sidebarContainer.getWidth());
                transition.setOnFinished(e -> {
                    sidebarContainer.setVisible(false);
                    sidebarContainer.setManaged(false);

                    if (toggleSidebarButton != null) {
                        toggleSidebarButton.setVisible(false);
                        toggleSidebarButton.setManaged(false);
                    }
                    if (toggleSidebarButtonOutside != null) {
                        toggleSidebarButtonOutside.setVisible(true);
                        toggleSidebarButtonOutside.setManaged(true);
                        toggleSidebarButtonOutside.setText("☰");
                    }
                });
            }
            transition.play();
        }
    }

    @FXML
    private void handleToggleDarkMode() {
        isDarkMode = !isDarkMode;

        Scene scene = MainApp.getPrimaryStage().getScene();
        javafx.scene.Parent root = scene.getRoot();

        if (isDarkMode) {
            if (!root.getStyleClass().contains("dark-mode")) {
                root.getStyleClass().add("dark-mode");
            }

            root.setStyle(
                    "-fx-base: #1a1a2e; " +
                            "-fx-background: #0f1419; " +
                            "-fx-control-inner-background: #1e2936; " +
                            "-fx-accent: #5865F2; " +
                            "-fx-background-color: linear-gradient(to bottom right, #0f1419 0%, #1a1a2e 100%);"
            );

            applyDarkModeStyles(root);

            if (darkModeButton != null) {
                darkModeButton.setText("☀️ Mode Clair");
                darkModeButton.setStyle(
                        "-fx-background-color: rgba(251, 191, 36, 0.1); " +
                                "-fx-text-fill: #fbbf24; " +
                                "-fx-font-size: 12px; " +
                                "-fx-padding: 10px; " +
                                "-fx-background-radius: 10px; " +
                                "-fx-cursor: hand; " +
                                "-fx-border-color: rgba(251, 191, 36, 0.4); " +
                                "-fx-border-width: 1.5px; " +
                                "-fx-border-radius: 10px;"
                );
            }
        } else {
            root.getStyleClass().remove("dark-mode");
            root.setStyle("");
            applyLightModeStyles(root);

            if (darkModeButton != null) {
                darkModeButton.setText("🌙 Mode Sombre");
                darkModeButton.setStyle("");
            }
        }
        loadUserHistory();
    }

    private void applyDarkModeStyles(javafx.scene.Parent parent) {
        if (parent == null) return;
        applyNodeDarkModeStyle(parent);
        if (parent instanceof javafx.scene.layout.Pane) {
            javafx.scene.layout.Pane pane = (javafx.scene.layout.Pane) parent;
            for (javafx.scene.Node child : pane.getChildren()) {
                if (child instanceof javafx.scene.Parent) {
                    applyDarkModeStyles((javafx.scene.Parent) child);
                } else {
                    applyNodeDarkModeStyle(child);
                }
            }
        }
    }

    private void applyLightModeStyles(javafx.scene.Parent parent) {
        if (parent == null) return;
        applyNodeLightModeStyle(parent);
        if (parent instanceof javafx.scene.layout.Pane) {
            javafx.scene.layout.Pane pane = (javafx.scene.layout.Pane) parent;
            for (javafx.scene.Node child : pane.getChildren()) {
                if (child instanceof javafx.scene.Parent) {
                    applyLightModeStyles((javafx.scene.Parent) child);
                } else {
                    applyNodeLightModeStyle(child);
                }
            }
        }
    }

    private void applyNodeDarkModeStyle(javafx.scene.Node node) {
        if (node == null) return;
        // Styles spécifiques (inchangés)
        if (node instanceof VBox && node.getStyleClass().contains("sidebar-container")) {
            node.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e 0%, #16213e 100%); " +
                    "-fx-border-color: transparent rgba(88, 101, 242, 0.3) transparent transparent; " +
                    "-fx-border-width: 0 2 0 0; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 15, 0, 5, 0);");
        }
        if (node.getStyleClass().contains("main-card-container")) {
            node.setStyle("-fx-background-color: rgba(30, 41, 54, 0.95); " +
                    "-fx-background-radius: 30px; " +
                    "-fx-border-color: rgba(88, 101, 242, 0.2); " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 30px; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 40, 0, 0, 10);");
        }
        if (node.getStyleClass().contains("actions-section-box")) {
            node.setStyle("-fx-background-color: linear-gradient(to bottom, #252f3f 0%, #1e2936 100%); " +
                    "-fx-background-radius: 20px; " +
                    "-fx-border-color: rgba(88, 101, 242, 0.25); " +
                    "-fx-border-width: 1.5px; " +
                    "-fx-border-radius: 20px;");
        }
        if (node.getStyleClass().contains("welcome-header")) {
            node.setStyle("-fx-background-color: rgba(30, 41, 54, 0.9); " +
                    "-fx-background-radius: 18px; " +
                    "-fx-border-color: rgba(88, 101, 242, 0.3); " +
                    "-fx-border-width: 1.5px; " +
                    "-fx-border-radius: 18px; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 15, 0, 0, 3);");
        }
        if (node instanceof Label) {
            Label label = (Label) node;
            if (label.getStyleClass().contains("main-title")) {
                label.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: #e8ecf1; -fx-padding: 10 0 5 0;");
            } else if (label.getStyleClass().contains("subtitle")) {
                label.setStyle("-fx-font-size: 16px; -fx-text-fill: #9ca3af; -fx-padding: 0 0 15 0;");
            } else if (label.getStyleClass().contains("welcome-message")) {
                label.setStyle("-fx-text-fill: #7289DA; -fx-font-weight: 600; -fx-font-size: 26px;");
            } else if (label.getStyleClass().contains("sidebar-title")) {
                label.setStyle("-fx-text-fill: #e8ecf1; -fx-font-weight: bold; -fx-font-size: 18px;");
            } else if (label.getStyleClass().contains("sidebar-username")) {
                label.setStyle("-fx-text-fill: #e8ecf1; -fx-font-weight: 600; -fx-font-size: 16px;");
            } else if (label.getStyleClass().contains("footer-label")) {
                label.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280; -fx-font-style: italic;");
            }
        }
        if (node.getStyleClass().contains("import-button")) {
            node.setStyle("-fx-background-color: #1e2936; " +
                    "-fx-text-fill: #e8ecf1; " +
                    "-fx-border-color: #5865F2; " +
                    "-fx-border-width: 2.5px; " +
                    "-fx-border-radius: 16px; " +
                    "-fx-background-radius: 16px; " +
                    "-fx-effect: dropshadow(gaussian, rgba(88, 101, 242, 0.3), 15, 0, 0, 5);");
        }
    }

    private void applyNodeLightModeStyle(javafx.scene.Node node) {
        if (node == null) return;
        if (node instanceof VBox && node.getStyleClass().contains("sidebar-container")) node.setStyle("");
        if (node.getStyleClass().contains("main-card-container")) node.setStyle("");
        if (node.getStyleClass().contains("actions-section-box")) node.setStyle("");
        if (node.getStyleClass().contains("welcome-header")) node.setStyle("");
        if (node instanceof Label) {
            Label label = (Label) node;
            if (label.getStyleClass().contains("main-title") || label.getStyleClass().contains("subtitle") ||
                    label.getStyleClass().contains("welcome-message") || label.getStyleClass().contains("sidebar-title") ||
                    label.getStyleClass().contains("sidebar-username") || label.getStyleClass().contains("footer-label")) {
                label.setStyle("");
            }
        }
        if (node.getStyleClass().contains("import-button")) node.setStyle("");
    }

    @FXML
    private void handleNewAnalysis() {
        selectedFile = null;
        if (fileLabel != null) {
            fileLabel.setText("");
            fileLabel.setVisible(false);
        }
        if (resultsContainer != null) {
            resultsContainer.setVisible(false);
            resultsContainer.setManaged(false);
            resultsContainer.getChildren().clear();
        }
        if (similarityResult != null) similarityResult.setText("0%");
        if (sendButton != null) sendButton.setDisable(true);
        if (importButton != null) importButton.setDisable(false);
        if (progressBar != null) {
            progressBar.setVisible(false);
            progressBar.setProgress(0);
        }
        if (progressLabel != null) {
            progressLabel.setVisible(false);
            progressLabel.setText("");
        }
        System.out.println("Interface réinitialisée pour une nouvelle analyse");
    }

    private void cleanupTask() {
        if (progressBar != null) {
            progressBar.setVisible(false);
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
        }
        if (progressLabel != null) {
            progressLabel.setVisible(false);
            progressLabel.textProperty().unbind();
            progressLabel.setText("");
        }
    }

    private void displayResults(Map<String, Object> result) {
        try {
            System.out.println("=== DEBUG: Résultat reçu ===");
            if (result.containsKey("hits")) {
                displayElasticsearchResult(result);
            } else {
                displayLegacyResult(result);
            }
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de l'affichage des résultats: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void displayElasticsearchResult(Map<String, Object> result) {
        try {
            Map<String, Object> hits = (Map<String, Object>) result.get("hits");
            List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hits.get("hits");

            if (hitsList == null || hitsList.isEmpty()) {
                showAlert("Information", "Aucun résultat d'analyse trouvé");
                return;
            }

            Map<String, Object> latestResult = hitsList.get(0);
            Map<String, Object> source = (Map<String, Object>) latestResult.get("_source");

            if (source == null) {
                showAlert("Erreur", "Données d'analyse invalides");
                return;
            }

            if (resultsContainer != null) {
                resultsContainer.getChildren().clear();
            }

            double overallSimilarity = ((Number) source.get("overall_similarity")).doubleValue();
            Map<String, Object> sectionSimilarities = (Map<String, Object>) source.get("section_similarities");
            String filename = (String) source.get("filename");
            List<Map<String, Object>> topMatches = (List<Map<String, Object>>) source.get("top_matches");

            Label reportTitle = new Label("📊 Rapport d'Analyse de Similarité");
            reportTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + getTitleColor() + "; -fx-padding: 0 0 10 0;");
            reportTitle.setMaxWidth(Double.MAX_VALUE);

            Label fileLabel = new Label("Fichier: " + filename);
            fileLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + getSubtitleColor() + "; -fx-padding: 0 0 20 0;");
            fileLabel.setMaxWidth(Double.MAX_VALUE);

            resultsContainer.getChildren().addAll(reportTitle, fileLabel);

            VBox globalSection = createGlobalSimilaritySection(overallSimilarity);
            resultsContainer.getChildren().add(globalSection);

            HBox riskSection = createGlobalRiskSection(overallSimilarity);
            resultsContainer.getChildren().add(riskSection);

            if (sectionSimilarities != null && !sectionSimilarities.isEmpty()) {
                VBox sectionsTable = createSectionsTable(sectionSimilarities);
                resultsContainer.getChildren().add(sectionsTable);
            } else {
                VBox noSectionsBox = new VBox(10);
                noSectionsBox.setStyle("-fx-background-color: " + getSectionBgColor() + "; -fx-background-radius: 12px; -fx-padding: 25px;");
                noSectionsBox.setMaxWidth(Double.MAX_VALUE);
                Label noSectionsTitle = new Label("Détails par Section");
                noSectionsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e8ecf1;");
                Label noSectionsMsg = new Label("Analyse détaillée par section non disponible.");
                noSectionsMsg.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e0;");
                noSectionsBox.getChildren().addAll(noSectionsTitle, noSectionsMsg);
                resultsContainer.getChildren().add(noSectionsBox);
            }

            if (topMatches != null && !topMatches.isEmpty()) {
                VBox similarDocsSection = createSimilarDocumentsSection(topMatches);
                resultsContainer.getChildren().add(similarDocsSection);
            }

            VBox recommendations = createRecommendationsSection(overallSimilarity, sectionSimilarities);
            resultsContainer.getChildren().add(recommendations);

            if (resultsContainer != null) {
                resultsContainer.setVisible(true);
                resultsContainer.setManaged(true);
            }

        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors du traitement des résultats Elasticsearch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void displayLegacyResult(Map<String, Object> result) {
        try {
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            if (data == null) data = result;
            Map<String, Object> analysis = (Map<String, Object>) data.get("analysis");
            if (analysis == null) analysis = data;

            if (resultsContainer != null) resultsContainer.getChildren().clear();

            Object similarityObj = analysis.get("overall_similarity");
            double overallSimilarity = 0.0;

            if (similarityObj != null) {
                if (similarityObj instanceof Number) {
                    overallSimilarity = ((Number) similarityObj).doubleValue();
                    if (overallSimilarity <= 1.0) overallSimilarity = overallSimilarity * 100;
                } else if (similarityObj instanceof String) {
                    try {
                        overallSimilarity = Double.parseDouble(((String) similarityObj).replace("%", ""));
                    } catch (NumberFormatException e) {
                        overallSimilarity = 0.0;
                    }
                }
            }

            Map<String, Object> sectionSimilarities = (Map<String, Object>) analysis.get("section_similarities");
            if (sectionSimilarities == null) sectionSimilarities = new HashMap<>();

            Label reportTitle = new Label("Rapport d'Analyse de Similarité et de Plagiat");
            reportTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + getTitleColor() + "; -fx-padding: 0 0 20 0;");
            reportTitle.setMaxWidth(Double.MAX_VALUE);
            resultsContainer.getChildren().add(reportTitle);

            resultsContainer.getChildren().add(createGlobalSimilaritySection(overallSimilarity));
            resultsContainer.getChildren().add(createGlobalRiskSection(overallSimilarity));

            if (sectionSimilarities != null && !sectionSimilarities.isEmpty()) {
                resultsContainer.getChildren().add(createSectionsTable(sectionSimilarities));
            } else {
                VBox noSectionsBox = new VBox(10);
                noSectionsBox.setStyle("-fx-background-color: " + getSectionBgColor() + "; -fx-background-radius: 12px; -fx-padding: 25px;");
                noSectionsBox.setMaxWidth(Double.MAX_VALUE);
                Label noSectionsTitle = new Label("Détails par Section");
                noSectionsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e8ecf1;");
                Label noSectionsMsg = new Label("Aucune analyse détaillée par section disponible.");
                noSectionsMsg.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e0;");
                noSectionsBox.getChildren().addAll(noSectionsTitle, noSectionsMsg);
                resultsContainer.getChildren().add(noSectionsBox);
            }

            if (analysis.containsKey("results") || analysis.containsKey("top_matches")) {
                Object matchesObj = analysis.get("results");
                if (matchesObj == null) matchesObj = analysis.get("top_matches");
                if (matchesObj instanceof List && !((List<?>) matchesObj).isEmpty()) {
                    resultsContainer.getChildren().add(createSimilarDocumentsSection((List<Map<String, Object>>) matchesObj));
                }
            }

            resultsContainer.getChildren().add(createRecommendationsSection(overallSimilarity, sectionSimilarities));

            if (resultsContainer != null) {
                resultsContainer.setVisible(true);
                resultsContainer.setManaged(true);
            }

        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de l'affichage des résultats: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private VBox createGlobalSimilaritySection(double similarity) {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: " + getSectionBgGradient() + "; -fx-background-radius: 12px; -fx-padding: 25px;");
        section.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(section, new Insets(0, 0, 20, 0));

        Label sectionTitle = new Label("📊 Taux de Similarité Global");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e8ecf1; -fx-padding: 0 0 15 0;");

        HBox contentBox = new HBox(30);
        contentBox.setAlignment(Pos.CENTER_LEFT);

        VBox chartBox = createDonutChart(similarity);

        VBox detailsBox = new VBox(12);
        detailsBox.setAlignment(Pos.CENTER_LEFT);

        Label percentLabel = new Label(String.format("%.1f%%", similarity));
        percentLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #e8ecf1;");

        String alertLevel = getAlertLevel(similarity);
        String alertColor = getAlertColor(similarity);

        HBox alertBadge = new HBox(8);
        alertBadge.setAlignment(Pos.CENTER_LEFT);
        Circle alertDot = new Circle(8);
        alertDot.setFill(Color.web(alertColor));

        DropShadow dotGlow = new DropShadow();
        dotGlow.setColor(Color.web(alertColor));
        dotGlow.setRadius(10);
        dotGlow.setSpread(0.5);
        alertDot.setEffect(dotGlow);

        Label alertText = new Label(alertLevel.toUpperCase());
        alertText.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + alertColor + ";");
        alertBadge.getChildren().addAll(alertDot, alertText);

        Label descLabel = new Label(getDescription(similarity));
        descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e0; -fx-wrap-text: true;");
        descLabel.setMaxWidth(350);
        descLabel.setWrapText(true);

        detailsBox.getChildren().addAll(percentLabel, alertBadge, new Region() {{ setPrefHeight(10); }}, descLabel);
        contentBox.getChildren().addAll(chartBox, detailsBox);
        section.getChildren().addAll(sectionTitle, contentBox);

        return section;
    }

    private VBox createDonutChart(double similarity) {
        double clampedSimilarity = Math.min(100.0, Math.max(0.0, similarity));
        VBox chartBox = new VBox();
        chartBox.setAlignment(Pos.CENTER);
        chartBox.setPrefSize(160, 160);

        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(160, 160);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();

        double centerX = 80;
        double centerY = 80;
        double radius = 60;
        double lineWidth = 16;
        String trackColor = isDarkMode ? "#2d3748" : "#e2e8f0";
        String progressColor = getAlertColor(clampedSimilarity);

        gc.setStroke(Color.web(trackColor));
        gc.setLineWidth(lineWidth);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.strokeArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 90.0, -360.0, javafx.scene.shape.ArcType.OPEN);

        gc.setStroke(Color.web(progressColor));
        gc.setLineWidth(lineWidth);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        double startAngle = 90.0;
        double arcAngle = -(clampedSimilarity / 100.0) * 360.0;
        gc.strokeArc(centerX - radius, centerY - radius, radius * 2, radius * 2, startAngle, arcAngle, javafx.scene.shape.ArcType.OPEN);

        StackPane stackPane = new StackPane();
        stackPane.setPrefSize(160, 160);
        stackPane.getChildren().add(canvas);

        Label centerText = new Label(String.format("%.1f%%", clampedSimilarity));
        String textColor = isDarkMode ? "#e8ecf1" : "#2d3748";
        centerText.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
        stackPane.getChildren().add(centerText);

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(progressColor));
        glow.setRadius(20);
        glow.setSpread(0.3);
        stackPane.setEffect(glow);

        chartBox.getChildren().add(stackPane);
        return chartBox;
    }

    private HBox createGlobalRiskSection(double similarity) {
        HBox section = new HBox(15);
        String bgColor = isDarkMode ? "#1e2936" : "#2c3e50";
        section.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8px; -fx-padding: 15px 20px;");
        section.setAlignment(Pos.CENTER_LEFT);
        section.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(section, new Insets(0, 0, 20, 0));

        Label riskLabel = new Label("NIVEAU GLOBAL DE RISQUE DE PLAGIAT :");
        riskLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e8ecf1;");

        Circle riskDot = new Circle(8);
        riskDot.setFill(Color.web(getAlertColor(similarity)));

        Label riskLevel = new Label(getAlertLevel(similarity).toUpperCase());
        riskLevel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + getAlertColor(similarity) + ";");

        section.getChildren().addAll(riskLabel, riskDot, riskLevel);
        return section;
    }

    @SuppressWarnings("unchecked")
    private VBox createSectionsTable(Map<String, Object> sectionSimilarities) {
        VBox tableSection = new VBox(10);
        String bgColor = isDarkMode ? "#1e2936" : "#3a4a6b";
        tableSection.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12px; -fx-padding: 25px;");
        tableSection.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(tableSection, new Insets(0, 0, 20, 0));

        Label tableTitle = new Label("📋 Détails par Section du Document");
        tableTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e8ecf1; -fx-padding: 0 0 15 0;");

        HBox headerRow = new HBox();
        String headerBg = isDarkMode ? "#252f3f" : "#2c3e50";
        headerRow.setStyle("-fx-background-color: " + headerBg + "; -fx-padding: 12px; -fx-background-radius: 6px;");
        headerRow.setSpacing(20);

        Label headerSection = new Label("Section");
        headerSection.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #cbd5e0; -fx-min-width: 200px;");
        Label headerSimilarity = new Label("Similarité Max");
        headerSimilarity.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #cbd5e0; -fx-min-width: 100px;");
        Label headerAverage = new Label("Moyenne");
        headerAverage.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #cbd5e0; -fx-min-width: 100px;");
        Label headerRisk = new Label("Risque");
        headerRisk.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #cbd5e0; -fx-min-width: 100px;");

        headerRow.getChildren().addAll(headerSection, headerSimilarity, headerAverage, headerRisk);

        VBox tableRows = new VBox(8);

        for (Map.Entry<String, Object> entry : sectionSimilarities.entrySet()) {
            String sectionKey = entry.getKey();
            Object sectionDataObj = entry.getValue();
            if (sectionDataObj instanceof Map) {
                addSectionTableRow(tableRows, sectionKey, (Map<String, Object>) sectionDataObj);
            }
        }

        Label noteLabel = new Label("* Les scores sont en pourcentage (0-100%)");
        noteLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #718096; -fx-padding: 10 0 0 0;");
        noteLabel.setWrapText(true);

        tableSection.getChildren().addAll(tableTitle, headerRow, tableRows, noteLabel);
        return tableSection;
    }

    private void addSectionTableRow(VBox container, String sectionKey, Map<String, Object> sectionData) {
        double maxScore = 0.0;
        double avgScore = 0.0;
        Object maxScoreObj = sectionData.get("max_score");
        Object avgScoreObj = sectionData.get("avg_score");
        if (maxScoreObj instanceof Number) maxScore = ((Number) maxScoreObj).doubleValue();
        if (avgScoreObj instanceof Number) avgScore = ((Number) avgScoreObj).doubleValue();

        String riskLevel = (String) sectionData.get("risk_level");
        if (riskLevel == null || riskLevel.isEmpty()) riskLevel = determineRiskLevel(maxScore);
        String sectionName = getFrenchSectionName(sectionKey);

        HBox row = new HBox();
        String rowBg = isDarkMode ? "#2a3544" : "#4a5f7f";
        row.setStyle("-fx-background-color: " + rowBg + "; -fx-padding: 12px; -fx-background-radius: 6px;");
        row.setSpacing(20);

        Label nameLabel = new Label(sectionName);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #e8ecf1; -fx-min-width: 200px;");

        HBox maxScoreBox = new HBox(8);
        maxScoreBox.setAlignment(Pos.CENTER_LEFT);
        maxScoreBox.setMinWidth(100);
        Circle maxIndicator = new Circle(5);
        maxIndicator.setFill(Color.web(getAlertColor(maxScore)));
        Label maxScoreLabel = new Label(String.format("%.1f%%", maxScore));
        maxScoreLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8ecf1;");
        maxScoreBox.getChildren().addAll(maxIndicator, maxScoreLabel);

        HBox avgScoreBox = new HBox(8);
        avgScoreBox.setAlignment(Pos.CENTER_LEFT);
        avgScoreBox.setMinWidth(100);
        Circle avgIndicator = new Circle(5);
        avgIndicator.setFill(Color.web(getAlertColor(avgScore)));
        Label avgScoreLabel = new Label(String.format("%.1f%%", avgScore));
        avgScoreLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e0;");
        avgScoreBox.getChildren().addAll(avgIndicator, avgScoreLabel);

        HBox riskBox = new HBox(8);
        riskBox.setAlignment(Pos.CENTER_LEFT);
        riskBox.setMinWidth(100);
        Circle riskDot = new Circle(5);
        riskDot.setFill(Color.web(getAlertColor(maxScore)));
        Label riskLabel = new Label(getRiskText(riskLevel));
        riskLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + getAlertColor(maxScore) + ";");
        riskBox.getChildren().addAll(riskDot, riskLabel);

        row.getChildren().addAll(nameLabel, maxScoreBox, avgScoreBox, riskBox);
        container.getChildren().add(row);
    }

    @SuppressWarnings("unchecked")
    private VBox createSimilarDocumentsSection(List<Map<String, Object>> similarDocuments) {
        VBox section = new VBox(10);
        String bgColor = isDarkMode ? "#1e2936" : "#3a4a6b";
        section.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12px; -fx-padding: 25px;");
        section.setMaxWidth(Double.MAX_VALUE);

        Label sectionTitle = new Label("📄 Top Documents Similaires");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e8ecf1; -fx-padding: 0 0 15 0;");
        section.getChildren().add(sectionTitle);
        VBox.setMargin(section, new Insets(0, 0, 20, 0));

        // --- DÉBUT MODIFICATION : TRI ET FILTRAGE ---
        List<Map<String, Object>> uniqueDocuments = new ArrayList<>();
        java.util.Set<String> seenKeys = new java.util.HashSet<>();

        if (similarDocuments != null) {
            // 1. Créer une copie modifiable de la liste pour éviter les erreurs
            List<Map<String, Object>> sortedDocs = new ArrayList<>(similarDocuments);

            // 2. TRIER la liste par score de similarité (Décroissant : du plus grand au plus petit)
            sortedDocs.sort((doc1, doc2) -> {
                double score1 = 0.0;
                double score2 = 0.0;

                Object s1 = doc1.get("similarity_score");
                Object s2 = doc2.get("similarity_score");

                if (s1 instanceof Number) score1 = ((Number) s1).doubleValue();
                if (s2 instanceof Number) score2 = ((Number) s2).doubleValue();

                // Double.compare(score2, score1) pour un ordre décroissant
                return Double.compare(score2, score1);
            });

            // 3. Filtrer les doublons sur la liste TRIÉE
            // Comme la liste est triée, on garde la première occurrence (qui a le meilleur score)
            for (Map<String, Object> doc : sortedDocs) {
                String docId = (String) doc.get("doc_id");
                String title = (String) doc.get("title");

                // Utiliser l'ID ou le titre comme clé unique
                String uniqueKey = (docId != null && !docId.isEmpty()) ? docId : title;

                if (uniqueKey != null && !seenKeys.contains(uniqueKey)) {
                    seenKeys.add(uniqueKey);
                    uniqueDocuments.add(doc);
                }
                // On s'arrête dès qu'on a 5 documents uniques
                if (uniqueDocuments.size() >= 5) break;
            }
        }
        // --- FIN MODIFICATION ---

        if (uniqueDocuments.isEmpty()) {
            Label noDocsLabel = new Label("Aucun document similaire trouvé.");
            noDocsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e0;");
            section.getChildren().add(noDocsLabel);
            return section;
        }

        for (int i = 0; i < uniqueDocuments.size(); i++) {
            Map<String, Object> doc = uniqueDocuments.get(i);
            HBox docBox = new HBox(15);
            String docBg = isDarkMode ? "#2a3544" : "#4a5f7f";
            docBox.setStyle("-fx-background-color: " + docBg + "; -fx-padding: 15px; -fx-background-radius: 8px;");
            docBox.setAlignment(Pos.CENTER_LEFT);

            VBox numberBox = new VBox();
            numberBox.setAlignment(Pos.CENTER);
            numberBox.setMinWidth(40);
            Circle numberCircle = new Circle(15);
            Object scoreObj = doc.get("similarity_score");
            double score = 0.0;
            if (scoreObj instanceof Number) score = ((Number) scoreObj).doubleValue();

            numberCircle.setFill(Color.web(getAlertColor(score)));
            Label numberLabel = new Label(String.valueOf(i + 1));
            numberLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
            StackPane circlePane = new StackPane();
            circlePane.getChildren().addAll(numberCircle, numberLabel);
            numberBox.getChildren().add(circlePane);

            VBox infoBox = new VBox(5);
            infoBox.setFillWidth(true);
            HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);
            String title = (String) doc.getOrDefault("title", "Document sans titre");
            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e8ecf1;");
            titleLabel.setWrapText(true);
            String docId = (String) doc.get("doc_id");
            if (docId != null) {
                Label idLabel = new Label("ID: " + docId.substring(0, Math.min(8, docId.length())) + "...");
                idLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ca3af;");
                infoBox.getChildren().add(idLabel);
            }
            infoBox.getChildren().add(0, titleLabel);

            VBox scoreBox = new VBox(5);
            scoreBox.setAlignment(Pos.CENTER_RIGHT);
            scoreBox.setMinWidth(100);
            Label scoreLabel = new Label(String.format("%.1f%%", score));
            scoreLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + getAlertColor(score) + ";");
            String riskLevel = (String) doc.get("risk_level");
            if (riskLevel != null) {
                Label riskLabel = new Label(getRiskText(riskLevel));
                riskLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + getAlertColor(score) + ";");
                scoreBox.getChildren().add(riskLabel);
            }
            scoreBox.getChildren().add(0, scoreLabel);

            docBox.getChildren().addAll(numberBox, infoBox, scoreBox);
            section.getChildren().add(docBox);
        }

        Label noteLabel = new Label("Basé sur l'algorithme: BM25 + MiniLM (hybrid)");
        noteLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #718096; -fx-padding: 10 0 0 0;");
        section.getChildren().addAll(noteLabel);
        return section;
    }
    private VBox createRecommendationsSection(double overallSimilarity, Map<String, Object> sectionSimilarities) {
        VBox recSection = new VBox(12);
        String bgColor = isDarkMode ? "#1e2936" : "#3a4a6b";
        recSection.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12px; -fx-padding: 25px;");
        recSection.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(recSection, new Insets(0, 0, 20, 0));

        Label recTitle = new Label("💡 Recommandations et Étapes Suivantes");
        recTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e8ecf1; -fx-padding: 0 0 10 0;");

        VBox recommendations = new VBox(10);

        if (overallSimilarity >= 70) {
            addRecommendation(recommendations, "Action Prioritaire : Réviser complètement le document", true);
            List<String> problematicSections = new ArrayList<>();
            if (sectionSimilarities != null) {
                for (Map.Entry<String, Object> entry : sectionSimilarities.entrySet()) {
                    Object sectionData = entry.getValue();
                    double sectionScore = 0.0;
                    if (sectionData instanceof Map) {
                        Object scoreObj = ((Map<String, Object>) sectionData).get("max_score");
                        if (scoreObj instanceof Number) sectionScore = ((Number) scoreObj).doubleValue();
                    }
                    if (sectionScore >= 70) problematicSections.add(getFrenchSectionName(entry.getKey()));
                }
            }
            if (!problematicSections.isEmpty()) {
                addRecommendation(recommendations, "Sections à réviser en priorité : " + String.join(", ", problematicSections), false);
            }
            addRecommendation(recommendations, "Vérification approfondie des passages similaires", false);
            addRecommendation(recommendations, "Ajouter des citations et références manquantes", false);
            addRecommendation(recommendations, "Paraphraser les passages trop similaires", false);
        } else if (overallSimilarity >= 40) {
            addRecommendation(recommendations, "Action Prioritaire : Vérifier les sections avec similarité modérée", true);
            addRecommendation(recommendations, "Examiner les passages identifiés comme similaires", false);
            addRecommendation(recommendations, "Vérifier que toutes les sources sont correctement citées", false);
            addRecommendation(recommendations, "Consulter votre superviseur pour validation", false);
        } else {
            addRecommendation(recommendations, "Vérification finale des citations et références", false);
            addRecommendation(recommendations, "S'assurer du respect des normes de rédaction", false);
            addRecommendation(recommendations, "Document prêt pour soumission", false);
        }

        recSection.getChildren().addAll(recTitle, recommendations);
        return recSection;
    }

    private void addRecommendation(VBox container, String text, boolean isPriority) {
        HBox recBox = new HBox(12);
        recBox.setAlignment(Pos.TOP_LEFT);
        recBox.setStyle("-fx-padding: 8px 0;");
        Label bullet = new Label("•");
        bullet.setStyle("-fx-font-size: 16px; -fx-text-fill: " + (isPriority ? "#f39c12" : "#5865F2") + "; -fx-font-weight: bold;");
        Label recText = new Label(text);
        recText.setStyle("-fx-font-size: 13px; -fx-text-fill: #e8ecf1; -fx-wrap-text: true;");
        recText.setWrapText(true);
        recText.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(recText, javafx.scene.layout.Priority.ALWAYS);
        recBox.getChildren().addAll(bullet, recText);
        container.getChildren().add(recBox);
    }

    // --- NOUVEAU SYSTÈME D'ALERTE MODERNE ---

    private void showModernAlert(String title, String message, Alert.AlertType type) {
        Stage alertStage = new Stage();
        alertStage.initOwner(MainApp.getPrimaryStage());
        alertStage.initModality(Modality.WINDOW_MODAL);
        alertStage.initStyle(StageStyle.TRANSPARENT);

        String accentColor;
        String iconSymbol;
        String titleColor = isDarkMode ? "#e8ecf1" : "#1a1a2e";
        String contentColor = isDarkMode ? "#cbd5e0" : "#4b5563";
        String bgColor = isDarkMode ? "#1e2936" : "#ffffff";
        String borderColor = isDarkMode ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)";

        switch (type) {
            case ERROR:
                accentColor = "#e74c3c";
                iconSymbol = "✕";
                break;
            case WARNING:
                accentColor = "#f39c12";
                iconSymbol = "⚠";
                break;
            case CONFIRMATION:
                accentColor = "#5865F2";
                iconSymbol = "?";
                break;
            default:
                accentColor = "#27ae60";
                iconSymbol = "✓";
                break;
        }

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + bgColor + ";" +
                "-fx-background-radius: 20px;" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 20px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.25), 20, 0, 0, 10);");

        StackPane iconPane = new StackPane();
        Circle iconCircle = new Circle(30);
        iconCircle.setFill(Color.TRANSPARENT);
        iconCircle.setStroke(Color.web(accentColor));
        iconCircle.setStrokeWidth(3);

        Label iconLabel = new Label(iconSymbol);
        iconLabel.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + ";");

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(accentColor));
        glow.setRadius(15);
        glow.setSpread(0.2);
        iconPane.setEffect(glow);
        iconPane.getChildren().addAll(iconCircle, iconLabel);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + titleColor + ";");

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setTextAlignment(TextAlignment.CENTER);
        msgLabel.setMaxWidth(300);
        msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + contentColor + ";");

        Button okButton = new Button("Compris");
        String btnStyle = "-fx-background-color: " + accentColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-padding: 10 30 10 30;" +
                "-fx-background-radius: 30px;" +
                "-fx-cursor: hand;";
        okButton.setStyle(btnStyle);

        okButton.setOnMouseEntered(e -> okButton.setStyle(btnStyle + "-fx-effect: dropshadow(gaussian, " + accentColor + ", 10, 0.3, 0, 0);"));
        okButton.setOnMouseExited(e -> okButton.setStyle(btnStyle));
        okButton.setOnAction(e -> alertStage.close());

        root.getChildren().addAll(iconPane, titleLabel, msgLabel, okButton);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        alertStage.setScene(scene);

        root.setScaleX(0.1);
        root.setScaleY(0.1);
        root.setOpacity(0);

        FadeTransition fade = new FadeTransition(Duration.millis(300), root);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(300), root);
        scaleTransition.setFromX(0.5);
        scaleTransition.setFromY(0.5);
        scaleTransition.setToX(1.0);
        scaleTransition.setToY(1.0);
        scaleTransition.setInterpolator(Interpolator.EASE_OUT);

        alertStage.setOnShown(e -> {
            fade.play();
            scaleTransition.play();
        });

        alertStage.showAndWait();
    }

    private boolean showModernConfirmation(String title, String message, String yesText, String noText) {
        final boolean[] result = {false};

        Stage dialogStage = new Stage();
        dialogStage.initOwner(MainApp.getPrimaryStage());
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);

        String bgColor = isDarkMode ? "#1e2936" : "#ffffff";
        String titleColor = isDarkMode ? "#e8ecf1" : "#1a1a2e";
        String contentColor = isDarkMode ? "#cbd5e0" : "#4b5563";

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + bgColor + ";" +
                "-fx-background-radius: 20px;" +
                "-fx-border-color: rgba(88, 101, 242, 0.3);" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 20px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 30, 0, 0, 10);");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + titleColor + ";");

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setTextAlignment(TextAlignment.CENTER);
        msgLabel.setMaxWidth(350);
        msgLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: " + contentColor + ";");

        HBox buttonsBox = new HBox(15);
        buttonsBox.setAlignment(Pos.CENTER);

        Button noButton = new Button(noText);
        String noBtnStyle = isDarkMode
                ? "-fx-background-color: transparent; -fx-border-color: #4b5563; -fx-border-radius: 30; -fx-text-fill: #9ca3af;"
                : "-fx-background-color: #f3f4f6; -fx-text-fill: #4b5563; -fx-background-radius: 30;";
        noButton.setStyle(noBtnStyle + "-fx-font-size: 14px; -fx-padding: 10 25; -fx-cursor: hand; -fx-font-weight: bold;");
        noButton.setOnMouseEntered(e -> noButton.setOpacity(0.8));
        noButton.setOnMouseExited(e -> noButton.setOpacity(1.0));

        Button yesButton = new Button(yesText);
        String yesStyle = "-fx-background-color: #5865F2;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-padding: 10 25;" +
                "-fx-background-radius: 30px;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(88, 101, 242, 0.4), 10, 0, 0, 4);";
        yesButton.setStyle(yesStyle);
        yesButton.setOnMouseEntered(e -> yesButton.setStyle(yesStyle + "-fx-background-color: #4752c4;"));
        yesButton.setOnMouseExited(e -> yesButton.setStyle(yesStyle));

        noButton.setOnAction(e -> {
            result[0] = false;
            dialogStage.close();
        });

        yesButton.setOnAction(e -> {
            result[0] = true;
            dialogStage.close();
        });

        buttonsBox.getChildren().addAll(noButton, yesButton);
        root.getChildren().addAll(titleLabel, msgLabel, buttonsBox);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(scene);

        root.setOpacity(0);
        root.setTranslateY(20);

        FadeTransition fade = new FadeTransition(Duration.millis(250), root);
        fade.setToValue(1);

        TranslateTransition move = new TranslateTransition(Duration.millis(250), root);
        move.setToY(0);

        dialogStage.setOnShown(e -> {
            fade.play();
            move.play();
        });

        dialogStage.showAndWait();
        return result[0];
    }

    private void showAlert(String title, String message) {
        if (title.toLowerCase().contains("erreur") || title.toLowerCase().contains("échec")) {
            showModernAlert(title, message, Alert.AlertType.ERROR);
        } else {
            showModernAlert(title, message, Alert.AlertType.INFORMATION);
        }
    }

    // --- HELPER METHODS ---

    private String getAlertLevel(double similarity) {
        if (similarity >= 70) return "Élevé";
        else if (similarity >= 40) return "Modéré";
        else return "Faible";
    }

    private String getAlertColor(double similarity) {
        if (similarity >= 70) return "#e74c3c";
        else if (similarity >= 40) return "#f39c12";
        else return "#27ae60";
    }

    private String getDescription(double similarity) {
        if (similarity >= 70)
            return "Niveau de plagiat élevé détecté. Plusieurs passages présentent une similarité importante avec des documents existants. Une révision approfondie est nécessaire.";
        else if (similarity >= 40)
            return "Similarité modérée. Certaines sections pourraient nécessiter des vérifications de citations et une reformulation partielle.";
        else if (similarity >= 20)
            return "Similarité faible. La plupart du contenu semble original. Vérifiez les citations manquantes.";
        else return "Similarité très faible. Le document présente un bon niveau d'originalité.";
    }

    private String getFrenchSectionName(String key) {
        switch (key.toLowerCase()) {
            case "introduction": return "Introduction Générale";
            case "abstract": return "Résumé (Abstract)";
            case "résumé": return "Résumé";
            case "conclusion": return "Conclusion Générale";
            case "chapters": return "Chapitres Principaux";
            case "chapter1": case "chapitre1": return "Chapitre 1";
            case "chapter2": case "chapitre2": return "Chapitre 2";
            case "chapter3": case "chapitre3": return "Chapitre 3";
            case "chapter4": case "chapitre4": return "Chapitre 4";
            case "methodology": return "Méthodologie";
            case "results": return "Résultats";
            case "discussion": return "Discussion";
            default:
                String formatted = key.replace("_", " ");
                return formatted.substring(0, 1).toUpperCase() + formatted.substring(1).toLowerCase();
        }
    }

    private String determineRiskLevel(double score) {
        if (score >= 70) return "Élevé";
        else if (score >= 40) return "Modéré";
        else return "Faible";
    }

    private String getRiskText(String riskLevel) {
        if (riskLevel == null) return "N/A";
        String lowerRisk = riskLevel.toLowerCase();
        if (lowerRisk.contains("élevé") || lowerRisk.contains("high") || lowerRisk.contains("eleve")) return "Élevé";
        else if (lowerRisk.contains("modéré") || lowerRisk.contains("modere") || lowerRisk.contains("medium") || lowerRisk.contains("moyen"))
            return "Modéré";
        else if (lowerRisk.contains("faible") || lowerRisk.contains("low")) return "Faible";
        else return riskLevel;
    }

    // Helpers pour les couleurs
    private String getTitleColor() { return isDarkMode ? "#e8ecf1" : "#1a1a2e"; }
    private String getSubtitleColor() { return isDarkMode ? "#9ca3af" : "#4b5563"; }
    private String getSectionBgColor() { return isDarkMode ? "#1e2936" : "#3a4a6b"; }
    private String getSectionBgGradient() { return isDarkMode ? "linear-gradient(to bottom right, #1e2936 0%, #252f3f 100%)" : "linear-gradient(to bottom right, #3a4a6b 0%, #2c3e5f 100%)"; }
}