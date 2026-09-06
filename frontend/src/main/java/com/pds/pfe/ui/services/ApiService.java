package com.pds.pfe.ui.services;

import com.pds.pfe.ui.utils.FrontendConstants;
import com.pds.pfe.ui.utils.TaskExecutor;
import com.pds.pfe.ui.utils.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ApiService {
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final String PYTHON_API_URL = "http://localhost:5000/api";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    // =================================================================
    // 1. MÉTHODES SYNCHRONES (Bloquantes)
    // Utilisées par SmartAnalysisTask pour faire le travail en arrière-plan
    // =================================================================

    /**
     * Méthode principale appelée par SmartAnalysisTask.
     * Elle orchestre l'upload PUIS l'analyse de façon synchrone.
     */
    public static Map<String, Object> analyzeFileSynchronous(File file) throws Exception {
        System.out.println("--- DÉBUT SÉQUENCE SYNCHRONE ---");

        // Étape 1 : Upload du fichier (Java Backend)
        Map<String, Object> uploadResult = uploadFileInternal(file);

        if (uploadResult == null || !uploadResult.containsKey("success") || !(Boolean) uploadResult.get("success")) {
            throw new RuntimeException("Échec de l'upload du fichier vers le serveur.");
        }

        Map<String, Object> data = (Map<String, Object>) uploadResult.get("data");
        String filePath = (String) data.get("filePath");
        String fileName = (String) data.get("fileName");

        System.out.println("--- UPLOAD TERMINÉ, DÉBUT ANALYSE ---");

        // Étape 2 : Analyse du fichier (Java -> Python)
        return analyzeRemoteFileInternal(filePath, fileName);
    }

    /**
     * Logique d'upload extraite (Multipart) - Sans Task
     */
    private static Map<String, Object> uploadFileInternal(File file) throws Exception {
        // Vérifications préliminaires
        if (!file.exists()) throw new IOException("Fichier introuvable");
        if (file.length() > FrontendConstants.MAX_FILE_SIZE_MB * 1024 * 1024) {
            throw new IOException("Fichier trop volumineux (> " + FrontendConstants.MAX_FILE_SIZE_MB + "MB)");
        }

        // Construction du corps Multipart manuellement (votre logique existante)
        String boundary = "----JavaFXUploadBoundary" + System.currentTimeMillis();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);

        writer.write("--" + boundary + "\r\n");
        writer.write("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n");
        writer.write("Content-Type: application/pdf\r\n\r\n");
        writer.flush();

        Files.copy(file.toPath(), outputStream);
        outputStream.flush();

        writer.write("\r\n--" + boundary + "--\r\n");
        writer.close();

        byte[] requestBody = outputStream.toByteArray();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return mapper.readValue(response.body(), Map.class);
        } else {
            throw new IOException("Erreur upload (" + response.statusCode() + "): " + response.body());
        }
    }

    /**
     * Logique d'analyse extraite - Sans Task
     */
    private static Map<String, Object> analyzeRemoteFileInternal(String filePath, String fileName) throws Exception {
        String userId = SessionManager.getCurrentUserId();

        Map<String, String> requestBodyMap = new HashMap<>();
        requestBodyMap.put("filePath", filePath);
        requestBodyMap.put("fileName", fileName);
        requestBodyMap.put("user_id", userId);

        String jsonBody = mapper.writeValueAsString(requestBodyMap);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/similarity/file"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return mapper.readValue(response.body(), Map.class);
        } else {
            // Gestion fine des erreurs
            if (response.statusCode() == 503) throw new IOException("Service d'analyse indisponible (vérifiez Python/Elasticsearch)");
            if (response.statusCode() == 400) throw new IOException("Erreur de validation des données");
            throw new IOException("Erreur analyse (" + response.statusCode() + "): " + response.body());
        }
    }

    // =================================================================
    // 2. TÂCHES JAVAFX (Asynchrones)
    // =================================================================

    /**
     * C'est LA méthode que votre DashboardController appelle.
     * Elle retourne maintenant une SmartAnalysisTask pour une barre de progression fluide.
     */
    public static Task<Map<String, Object>> uploadAndAnalyzeTask(File selectedFile) {
        // Retourne la tâche intelligente définie dans l'autre fichier
        // Assurez-vous d'avoir créé la classe SmartAnalysisTask comme indiqué précédemment
        return new SmartAnalysisTask(selectedFile);
    }

    // --- Les autres tâches (Login, Register, History) restent inchangées ---

    public static Task<Boolean> registerTask(String fullName, String email, String password) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("username", fullName);
                map.put("email", email);
                map.put("password", password);
                String json = mapper.writeValueAsString(map);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/auth/register"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 201) return true;
                if (response.statusCode() == 409) throw new RuntimeException("Email déjà utilisé");
                throw new RuntimeException("Erreur inscription: " + response.statusCode());
            }
        };
        TaskExecutor.executeTask(task);
        return task;
    }

    public static Task<Map<String, Object>> loginTask(String username, String password) {
        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                Map<String, String> map = new HashMap<>();
                map.put("username", username);
                map.put("password", password);
                String json = mapper.writeValueAsString(map);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return mapper.readValue(response.body(), Map.class);
                }
                throw new RuntimeException("Identifiants incorrects");
            }
        };
        TaskExecutor.executeTask(task);
        return task;
    }

    // Tâche simple pour l'upload seul (si besoin)
    public static Task<Map<String, Object>> uploadFileTask(File file) {
        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                updateMessage("Upload en cours...");
                // Réutilisation de la méthode synchrone
                return uploadFileInternal(file);
            }
        };
        TaskExecutor.executeTask(task);
        return task;
    }

    // Tâche pour l'historique
    public static Task<Map<String, Object>> getHistoryTask(String userId) {
        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(PYTHON_API_URL + "/history/" + userId))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) return mapper.readValue(response.body(), Map.class);
                throw new IOException("Erreur historique");
            }
        };
        TaskExecutor.executeTask(task);
        return task;
    }

    public static Task<Map<String, Object>> getHistoryResultTask(String resultId) {
        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(PYTHON_API_URL + "/history/result/" + resultId))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) return mapper.readValue(response.body(), Map.class);
                throw new IOException("Erreur résultat historique");
            }
        };
        TaskExecutor.executeTask(task);
        return task;
    }

    public static Task<Boolean> deleteHistoryItemTask(String resultId) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(PYTHON_API_URL + "/history/result/" + resultId))
                        .DELETE()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) return true;
                throw new RuntimeException("Erreur suppression");
            }
        };
        TaskExecutor.executeTask(task);
        return task;
    }
}