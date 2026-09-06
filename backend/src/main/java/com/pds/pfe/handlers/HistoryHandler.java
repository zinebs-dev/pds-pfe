package com.pds.pfe.handlers;

import com.pds.pfe.models.ApiResponse;
import com.pds.pfe.utils.JsonUtils;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HistoryHandler implements HttpHandler {
    private final HttpClient httpClient;

    public HistoryHandler() {
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        configureCORS(exchange);

        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                // GET /api/history/{user_id}
                handleGetHistory(exchange, path);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                // DELETE /api/history/{result_id}
                handleDeleteHistory(exchange, path);
            } else if ("OPTIONS".equalsIgnoreCase(method)) {
                handleOptions(exchange);
            } else {
                sendError(exchange, 405, "Méthode non autorisée");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Erreur interne: " + e.getMessage());
        }
    }

    private void handleGetHistory(HttpExchange exchange, String path) throws IOException {
        try {
            // Extraire le user_id du path: /api/history/{user_id}
            String[] pathParts = path.split("/");
            if (pathParts.length < 4) {
                sendError(exchange, 400, "user_id manquant dans le path");
                return;
            }

            String userId = pathParts[3];
            System.out.println("Récupération de l'historique pour l'utilisateur: " + userId);

            // Appeler l'API Python
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:5000/api/history/" + userId))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("API Python - Status: " + response.statusCode());

            if (response.statusCode() == 200) {
                Map<String, Object> result = JsonUtils.fromJson(response.body(), Map.class);
                ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(
                        "Historique récupéré",
                        result
                );
                sendResponse(exchange, 200, JsonUtils.toJson(apiResponse));
            } else {
                sendError(exchange, response.statusCode(), "Erreur API Python: " + response.body());
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Erreur lors de la récupération de l'historique: " + e.getMessage());
        }
    }

    private void handleDeleteHistory(HttpExchange exchange, String path) throws IOException {
        try {
            // Extraire le result_id du path: /api/history/{result_id}
            String[] pathParts = path.split("/");
            if (pathParts.length < 4) {
                sendError(exchange, 400, "result_id manquant dans le path");
                return;
            }

            String resultId = pathParts[3];
            System.out.println("Suppression de l'élément d'historique: " + resultId);

            // Appeler l'API Python
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:5000/api/history/" + resultId))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("API Python - Status: " + response.statusCode());

            if (response.statusCode() == 200) {
                Map<String, Object> result = JsonUtils.fromJson(response.body(), Map.class);
                ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(
                        "Élément supprimé",
                        result
                );
                sendResponse(exchange, 200, JsonUtils.toJson(apiResponse));
            } else {
                sendError(exchange, response.statusCode(), "Erreur API Python: " + response.body());
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Erreur lors de la suppression: " + e.getMessage());
        }
    }

    private void handleOptions(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(200, -1);
    }

    private void configureCORS(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        ApiResponse<String> errorResponse = ApiResponse.error(message);
        String jsonResponse = JsonUtils.toJson(errorResponse);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}

