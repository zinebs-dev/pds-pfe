package com.pds.pfe.handlers;

import com.pds.pfe.services.PythonSimilarityService;
import com.pds.pfe.services.ElasticsearchLoggerService;
import com.pds.pfe.models.*;
import com.pds.pfe.utils.JsonUtils;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class SimilarityHandler implements HttpHandler {
    private final PythonSimilarityService pythonService;
    private final HttpClient httpClient;
    private final ElasticsearchLoggerService loggerService;

    public SimilarityHandler() {
        this.pythonService = new PythonSimilarityService();
        this.httpClient = HttpClient.newHttpClient();
        this.loggerService = ElasticsearchLoggerService.getInstance();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        configureCORS(exchange);

        String requestId = null;
        User currentUser = null;

        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            // Récupérer l'utilisateur depuis les headers ou session
            currentUser = extractUserFromRequest(exchange);

            if ("POST".equalsIgnoreCase(method)) {
                if (path.endsWith("/similarity")) {
                    requestId = handleSimilarityCheck(exchange, currentUser);
                } else if (path.endsWith("/similarity/file")) {
                    requestId = handleSimilarityFromFileNew(exchange, currentUser); // Utiliser le nouveau nom
                } else {
                    sendError(exchange, 404, "Endpoint non trouvé: " + path, null, null);
                }
            } else if ("GET".equalsIgnoreCase(method)) {
                if (path.endsWith("/similarity/status")) {
                    handleGetStatus(exchange);
                } else if (path.endsWith("/similarity/health")) {
                    handlePythonHealthCheck(exchange);
                } else {
                    sendError(exchange, 404, "Endpoint non trouvé: " + path, null, null);
                }
            } else if ("OPTIONS".equalsIgnoreCase(method)) {
                handleOptions(exchange);
            } else {
                sendError(exchange, 405, "Méthode non autorisée", null, null);
            }
        } catch (Exception e) {
            // Log erreur
            loggerService.logResponseAsync(requestId,
                    currentUser != null ? currentUser.getId() : "unknown",
                    "SIMILARITY_ERROR",
                    500,
                    e.getMessage()
            );

            sendError(exchange, 500, "Erreur interne: " + e.getMessage(), requestId, currentUser);
        }
    }

    // === Méthode pour l'analyse de texte ===
    private String handleSimilarityCheck(HttpExchange exchange, User currentUser) throws IOException {
        InputStream requestBody = exchange.getRequestBody();
        String json = new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);

        System.out.println("=== BACKEND: Requête similarity reçue ===");
        System.out.println("JSON: " + json);

        AnalysisRequest request = JsonUtils.fromJson(json, AnalysisRequest.class);

        // Extraire l'utilisateur du JSON si disponible
        Map<String, Object> jsonData = JsonUtils.fromJson(json, Map.class);
        User userFromJson = extractUserFromJson(jsonData);
        if (userFromJson != null) {
            currentUser = userFromJson;
        }

        if (request.getText() == null || request.getText().trim().isEmpty()) {
            sendError(exchange, 400, "Le texte est requis", null, currentUser);
            return null;
        }

        // Log REQUEST
        AnalysisRequestLog requestLog = new AnalysisRequestLog(
                currentUser != null ? currentUser.getId() : "unknown",
                currentUser != null ? currentUser.getUsername() : "anonymous",
                "TEXT_ANALYSIS_REQUEST",
                null,
                null
        );

        // Optionnel: ajouter IP
        requestLog.setClientIp(exchange.getRemoteAddress().getAddress().getHostAddress());

        String requestId = requestLog.getRequestId();
        loggerService.logRequestAsync(requestLog);

        try {
            // Utiliser le service Python
            int topK = request.getTopK() != null ? request.getTopK() : 10;
            String userId = currentUser != null ? currentUser.getId() : "unknown";
            String username = currentUser != null ? currentUser.getUsername() : "anonymous";

            Map<String, Object> pythonResult = pythonService.analyzeText(
                request.getText(), topK, userId, username, requestId
            );

            // Log SUCCESS
            loggerService.logResponseAsync(
                    requestId,
                    userId,
                    "TEXT_ANALYSIS_COMPLETE",
                    200,
                    null
            );

            ApiResponse<Map<String, Object>> response = ApiResponse.success(
                    "Analyse terminée",
                    pythonResult
            );

            sendResponse(exchange, 200, JsonUtils.toJson(response), requestId);
            return requestId;

        } catch (Exception e) {
            // Log ERROR
            loggerService.logResponseAsync(
                    requestId,
                    currentUser != null ? currentUser.getId() : "unknown",
                    "TEXT_ANALYSIS_ERROR",
                    500,
                    e.getMessage()
            );
            throw new IOException("Erreur lors de l'analyse: " + e.getMessage(), e);
        }
    }

    // === NOUVELLE méthode pour l'analyse depuis fichier (avec journalisation) ===
    private String handleSimilarityFromFileNew(HttpExchange exchange, User currentUser) throws IOException {
        InputStream requestBody = exchange.getRequestBody();
        String json = new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);

        System.out.println("=== BACKEND: Analyse depuis fichier (nouvelle) ===");
        System.out.println("JSON: " + json);

        Map<String, String> request = JsonUtils.fromJson(json, Map.class);

        // Extraire l'utilisateur du JSON si disponible
        Map<String, Object> jsonData = JsonUtils.fromJson(json, Map.class);
        User userFromJson = extractUserFromJson(jsonData);
        if (userFromJson != null) {
            currentUser = userFromJson;
        }

        String filePath = request.get("filePath");
        String fileName = request.get("fileName");
        String userId = request.get("user_id");

        if (filePath == null || fileName == null) {
            sendError(exchange, 400, "filePath et fileName sont requis", null, currentUser);
            return null;
        }

        if (userId == null || userId.trim().isEmpty()) {
            userId = currentUser != null ? currentUser.getId() : "unknown";
        }

        System.out.println("Fichier à analyser: " + filePath);

        // Log REQUEST
        AnalysisRequestLog requestLog = new AnalysisRequestLog(
                userId,
                currentUser != null ? currentUser.getUsername() : "unknown",
                "FILE_ANALYSIS_REQUEST",
                fileName,
                null
        );

        String requestId = requestLog.getRequestId();
        loggerService.logRequestAsync(requestLog);

        // 1. Vérifier que le fichier existe
        Path pdfPath = Paths.get(filePath);
        if (!Files.exists(pdfPath)) {
            sendError(exchange, 404, "Fichier non trouvé: " + filePath, requestId, currentUser);
            return requestId;
        }

        long startTime = System.currentTimeMillis();

        try {
            // 2. Envoyer le fichier à l'API Python pour extraction et analyse
            Map<String, Object> result = sendFileToPythonApi(pdfPath, fileName, userId, requestId);

            long processingTime = System.currentTimeMillis() - startTime;

            // 3. Log SERVICE
            logFileAnalysisService(result, userId, currentUser != null ? currentUser.getUsername() : "anonymous",
                                   fileName, requestId, processingTime);

            // 4. Log RESPONSE SUCCESS
            loggerService.logResponseAsync(
                    requestId,
                    userId,
                    "FILE_ANALYSIS_COMPLETE",
                    200,
                    null
            );

            // 5. Répondre au client
            ApiResponse<Map<String, Object>> response = ApiResponse.success(
                    "Analyse depuis fichier terminée",
                    result
            );

            sendResponse(exchange, 200, JsonUtils.toJson(response), requestId);
            return requestId;

        } catch (Exception e) {
            // Log ERROR
            loggerService.logResponseAsync(
                    requestId,
                    userId,
                    "FILE_ANALYSIS_ERROR",
                    500,
                    e.getMessage()
            );
            throw new IOException("Erreur lors de l'analyse du fichier: " + e.getMessage(), e);
        }
    }

    // === ANCIENNE méthode pour l'analyse depuis fichier (gardée pour compatibilité) ===
    private void handleSimilarityFromFileOld(HttpExchange exchange) throws IOException {
        try {
            InputStream requestBody = exchange.getRequestBody();
            String json = new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);

            System.out.println("=== BACKEND: Analyse depuis fichier (ancienne) ===");
            System.out.println("JSON: " + json);

            Map<String, String> request = JsonUtils.fromJson(json, Map.class);
            String filePath = request.get("filePath");
            String fileName = request.get("fileName");
            String userId = request.get("user_id");

            if (filePath == null || fileName == null) {
                sendErrorOld(exchange, 400, "filePath et fileName sont requis");
                return;
            }

            if (userId == null || userId.trim().isEmpty()) {
                sendErrorOld(exchange, 400, "userId est requis");
                return;
            }

            System.out.println("Fichier à analyser: " + filePath);

            // 1. Vérifier que le fichier existe
            Path pdfPath = Paths.get(filePath);
            if (!Files.exists(pdfPath)) {
                sendErrorOld(exchange, 404, "Fichier non trouvé: " + filePath);
                return;
            }

            // 2. Envoyer le fichier à l'API Python pour extraction et analyse
            Map<String, Object> result = sendFileToPythonApiOld(pdfPath, fileName, userId);

            // 3. Répondre au client
            ApiResponse<Map<String, Object>> response = ApiResponse.success(
                    "Analyse depuis fichier terminée",
                    result
            );

            sendResponseOld(exchange, 200, JsonUtils.toJson(response));

        } catch (Exception e) {
            e.printStackTrace();
            sendErrorOld(exchange, 500, "Erreur lors de l'analyse du fichier: " + e.getMessage());
        }
    }

    // === Méthode d'envoi à l'API Python avec requestId (nouvelle version) ===
    private Map<String, Object> sendFileToPythonApi(Path pdfPath, String fileName, String userId, String requestId) throws Exception {
        System.out.println("=== Envoi à l'API Python (avec requestId) ===");
        System.out.println("Fichier: " + fileName);
        System.out.println("User ID: " + userId);
        System.out.println("Request ID: " + requestId);

        // Lire le fichier
        byte[] fileBytes = Files.readAllBytes(pdfPath);
        System.out.println("Taille fichier: " + fileBytes.length + " bytes");

        // Créer la requête multipart
        String boundary = "JavaPythonBoundary" + System.currentTimeMillis();

        // Construction du corps multipart
        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();

        // 1. Partie fichier
        String fileHeader = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                "Content-Type: application/pdf\r\n\r\n";
        bodyStream.write(fileHeader.getBytes(StandardCharsets.UTF_8));
        bodyStream.write(fileBytes);
        bodyStream.write("\r\n".getBytes(StandardCharsets.UTF_8));

        // 2. Partie user_id
        String userIdPart = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"user_id\"\r\n\r\n" +
                userId + "\r\n";
        bodyStream.write(userIdPart.getBytes(StandardCharsets.UTF_8));

        // 3. Partie request_id (pour la corrélation)
        String requestIdPart = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"request_id\"\r\n\r\n" +
                requestId + "\r\n";
        bodyStream.write(requestIdPart.getBytes(StandardCharsets.UTF_8));

        // 4. Partie filename
        String filenamePart = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"filename\"\r\n\r\n" +
                fileName + "\r\n";
        bodyStream.write(filenamePart.getBytes(StandardCharsets.UTF_8));

        // 5. Fin du multipart
        String endBoundary = "--" + boundary + "--\r\n";
        bodyStream.write(endBoundary.getBytes(StandardCharsets.UTF_8));

        byte[] requestBody = bodyStream.toByteArray();

        // Envoyer à l'API Python
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:5000/api/extract-and-analyze"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();

        System.out.println("Envoi de la requête à Python...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Réponse Python - Status: " + response.statusCode());
        System.out.println("Réponse Python - Body: " + response.body());

        if (response.statusCode() == 200) {
            return JsonUtils.fromJson(response.body(), Map.class);
        } else {
            throw new RuntimeException("API Python erreur: " + response.statusCode() + " - " + response.body());
        }
    }

    // === Méthode d'envoi à l'API Python sans requestId (ancienne version) ===
    private Map<String, Object> sendFileToPythonApiOld(Path pdfPath, String fileName, String userId) throws Exception {
        System.out.println("=== Envoi à l'API Python (ancienne) ===");
        System.out.println("Fichier: " + fileName);
        System.out.println("User ID: " + userId);

        // Lire le fichier
        byte[] fileBytes = Files.readAllBytes(pdfPath);
        System.out.println("Taille fichier: " + fileBytes.length + " bytes");

        // Créer la requête multipart
        String boundary = "JavaPythonBoundary" + System.currentTimeMillis();

        // Construction du corps multipart
        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();

        // 1. Partie fichier
        String fileHeader = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                "Content-Type: application/pdf\r\n\r\n";
        bodyStream.write(fileHeader.getBytes(StandardCharsets.UTF_8));
        bodyStream.write(fileBytes);
        bodyStream.write("\r\n".getBytes(StandardCharsets.UTF_8));

        // 2. Partie user_id
        String userIdPart = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"user_id\"\r\n\r\n" +
                userId + "\r\n";
        bodyStream.write(userIdPart.getBytes(StandardCharsets.UTF_8));
        System.out.println("user_id ajouté au multipart: " + userId);

        // 3. Partie filename
        String filenamePart = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"filename\"\r\n\r\n" +
                fileName + "\r\n";
        bodyStream.write(filenamePart.getBytes(StandardCharsets.UTF_8));

        // 4. Fin du multipart
        String endBoundary = "--" + boundary + "--\r\n";
        bodyStream.write(endBoundary.getBytes(StandardCharsets.UTF_8));

        byte[] requestBody = bodyStream.toByteArray();
        System.out.println("Taille requête totale: " + requestBody.length + " bytes");

        // Envoyer à l'API Python
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:5000/api/extract-and-analyze"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();

        System.out.println("Envoi de la requête à Python...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Réponse Python - Status: " + response.statusCode());
        System.out.println("Réponse Python - Body: " + response.body());

        if (response.statusCode() == 200) {
            return JsonUtils.fromJson(response.body(), Map.class);
        } else {
            throw new RuntimeException("API Python erreur: " + response.statusCode() + " - " + response.body());
        }
    }

    // === Méthodes utilitaires existantes ===
    private void handlePythonHealthCheck(HttpExchange exchange) throws IOException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:5000/api/health"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            Map<String, Object> result = new HashMap<>();
            result.put("python_api_status", response.statusCode() == 200 ? "UP" : "DOWN");
            result.put("python_api_response", response.body());

            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success("Statut API Python", result);
            sendResponseOld(exchange, 200, JsonUtils.toJson(apiResponse));

        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("python_api_status", "DOWN");
            result.put("python_api_error", e.getMessage());

            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success("Statut API Python", result);
            sendResponseOld(exchange, 200, JsonUtils.toJson(apiResponse));
        }
    }

    private void handleGetStatus(HttpExchange exchange) throws IOException {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("similarity_service", "ACTIVE");
            status.put("python_integration", "ENABLED");

            // Vérifier la connexion à l'API Python
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:5000/api/health"))
                        .timeout(java.time.Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                status.put("python_api", response.statusCode() == 200 ? "CONNECTED" : "DISCONNECTED");
            } catch (Exception e) {
                status.put("python_api", "DISCONNECTED");
                status.put("python_api_error", e.getMessage());
            }

            ApiResponse<Map<String, Object>> response = ApiResponse.success("Statut du service", status);
            sendResponseOld(exchange, 200, JsonUtils.toJson(response));

        } catch (Exception e) {
            sendErrorOld(exchange, 500, "Erreur lors de la récupération du statut: " + e.getMessage());
        }
    }

    private void handleOptions(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(200, -1);
    }

    private void configureCORS(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    // === Anciennes méthodes de réponse (sans requestId) ===
    private void sendResponseOld(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void sendErrorOld(HttpExchange exchange, int statusCode, String message) throws IOException {
        ApiResponse<String> errorResponse = ApiResponse.error(message);
        String jsonResponse = JsonUtils.toJson(errorResponse);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    // === Nouvelles méthodes de réponse (avec requestId) ===
    private void sendResponse(HttpExchange exchange, int statusCode, String response, String requestId) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message, String requestId, User user) throws IOException {
        // Log ERROR
        if (requestId != null && user != null) {
            loggerService.logResponseAsync(
                    requestId,
                    user.getId(),
                    "ERROR_" + statusCode,
                    statusCode,
                    message
            );
        }

        ApiResponse<String> errorResponse = ApiResponse.error(message);
        String jsonResponse = JsonUtils.toJson(errorResponse);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    // === Méthode d'extraction d'utilisateur ===
    @SuppressWarnings("unchecked")
    private void logFileAnalysisService(Map<String, Object> result, String userId, String username,
                                        String fileName, String requestId, long processingTime) {
        try {
            double maxScore = 0.0;
            int sourcesFound = 0;
            String algorithmUsed = "Unknown";

            System.out.println("=== [SERVICE LOG] Extraction métriques ===");

            // 1. DEBUG: Afficher toutes les clés disponibles
            System.out.println("Clés disponibles dans le résultat: " + result.keySet());

            // 2. Extraire overall_similarity (score max principal)
            if (result.containsKey("overall_similarity")) {
                Object overallObj = result.get("overall_similarity");
                if (overallObj instanceof Number) {
                    maxScore = ((Number) overallObj).doubleValue();
                    System.out.println("✓ overall_similarity trouvé: " + maxScore);
                } else {
                    System.out.println("✗ overall_similarity n'est pas un nombre: " + overallObj);
                }
            } else {
                System.out.println("✗ overall_similarity non trouvé");
            }

            // 3. Extraire total_matches (nombre total de sources trouvées)
            if (result.containsKey("total_matches")) {
                Object matchesObj = result.get("total_matches");
                if (matchesObj instanceof Number) {
                    sourcesFound = ((Number) matchesObj).intValue();
                    System.out.println("✓ total_matches trouvé: " + sourcesFound);
                } else {
                    System.out.println("✗ total_matches n'est pas un nombre: " + matchesObj);
                }
            } else {
                System.out.println("✗ total_matches non trouvé");
            }

            // 4. Si total_matches n'existe pas, compter top_matches
            if (sourcesFound == 0 && result.containsKey("top_matches")) {
                Object topMatchesObj = result.get("top_matches");
                if (topMatchesObj instanceof List) {
                    List<?> topMatches = (List<?>) topMatchesObj;
                    sourcesFound = topMatches.size();
                    System.out.println("✓ Sources comptées depuis top_matches: " + sourcesFound);

                    // Extraire aussi le score max de top_matches
                    List<Map<String, Object>> matchesList = (List<Map<String, Object>>) topMatchesObj;
                    for (Map<String, Object> match : matchesList) {
                        if (match.containsKey("similarity_score")) {
                            Object scoreObj = match.get("similarity_score");
                            if (scoreObj instanceof Number) {
                                double score = ((Number) scoreObj).doubleValue();
                                if (score > maxScore) {
                                    maxScore = score;
                                }
                            }
                        }
                    }
                    System.out.println("✓ Score max depuis top_matches: " + maxScore);
                }
            }

            // 5. Extraire algorithm_used
            if (result.containsKey("algorithm_used")) {
                Object algoObj = result.get("algorithm_used");
                if (algoObj != null) {
                    algorithmUsed = algoObj.toString();
                    System.out.println("✓ algorithm_used trouvé: " + algorithmUsed);
                }
            } else {
                System.out.println("✗ algorithm_used non trouvé, valeur par défaut: " + algorithmUsed);
            }

            // 6. Fallback: si toujours 0, chercher dans les sections
            if (sourcesFound == 0 && result.containsKey("section_similarities")) {
                try {
                    Map<String, Object> sections = (Map<String, Object>) result.get("section_similarities");
                    for (Object sectionObj : sections.values()) {
                        if (sectionObj instanceof Map) {
                            Map<String, Object> section = (Map<String, Object>) sectionObj;
                            if (section.containsKey("matches")) {
                                Object matchesObj = section.get("matches");
                                if (matchesObj instanceof List) {
                                    sourcesFound += ((List<?>) matchesObj).size();
                                }
                            }
                        }
                    }
                    if (sourcesFound > 0) {
                        System.out.println("✓ Sources trouvées dans section_similarities: " + sourcesFound);
                    }
                } catch (Exception e) {
                    System.err.println("Erreur section_similarities: " + e.getMessage());
                }
            }

            // 7. Résumé final
            System.out.println("=== [SERVICE LOG] Métriques finales ===");
            System.out.println("maxScore: " + maxScore);
            System.out.println("sourcesFound: " + sourcesFound);
            System.out.println("algorithmUsed: " + algorithmUsed);
            System.out.println("processingTime: " + processingTime + "ms");

            // 8. Créer le log SERVICE
            AnalysisServiceLog serviceLog = new AnalysisServiceLog(
                    userId,
                    username,
                    "FILE_ANALYSIS_SERVICE",
                    fileName,
                    null, // pdfId optionnel
                    requestId
            );

            serviceLog.setMaxSimilarityScore(maxScore);
            serviceLog.setNumberOfSourcesFound(sourcesFound);
            serviceLog.setProcessingTimeMs(processingTime);
            serviceLog.setAlgorithmUsed(algorithmUsed);
            serviceLog.setHttpStatus(200); // Important pour Kibana

            // 9. Envoyer au logger service
            loggerService.logServiceAsync(serviceLog);

            System.out.println("✓ Log SERVICE envoyé pour requestId: " + requestId);
            System.out.println("==============================================");

        } catch (Exception e) {
            System.err.println("Erreur lors de la journalisation SERVICE: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private User extractUserFromRequest(HttpExchange exchange) {
        // Ne PAS lire le body ici car il sera lu dans les handlers
        // Retourner un utilisateur par défaut basé sur l'IP
        String remoteIp = exchange.getRemoteAddress().getAddress().getHostAddress();
        String timestamp = String.valueOf(System.currentTimeMillis());
        return new User("ip_" + remoteIp, "user_" + timestamp.substring(7, 11), null);
    }

    private User extractUserFromJson(Map<String, Object> data) {
        try {
            if (data != null) {
                String userId = null;
                String username = null;

                // Chercher userId dans différents champs possibles
                userId = (String) data.get("user_id");
                if (userId == null) userId = (String) data.get("userId");
                if (userId == null && data.containsKey("user")) {
                    Map<String, Object> user = (Map<String, Object>) data.get("user");
                    if (user != null) {
                        userId = (String) user.get("id");
                        username = (String) user.get("username");
                    }
                }

                // Si userId trouvé mais pas username
                if (userId != null && (username == null || username.trim().isEmpty())) {
                    if (userId.length() > 8) {
                        username = "user_" + userId.substring(0, 8);
                    } else {
                        username = "user_" + userId;
                    }
                }

                if (userId != null && username != null) {
                    System.out.println("Utilisateur extrait du JSON: userId=" + userId + ", username=" + username);
                    return new User(userId, username, null);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur extraction utilisateur depuis JSON: " + e.getMessage());
        }
        return null;
    }
}