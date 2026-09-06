package com.pds.pfe.handlers;

import com.pds.pfe.models.ApiResponse;
import com.pds.pfe.models.Document;
import com.pds.pfe.utils.JsonUtils;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DocumentHandler implements HttpHandler {

    private final List<Document> documents = new ArrayList<>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        configureCORS(exchange);

        try {
            String path = exchange.getRequestURI().getPath();

            if (path.endsWith("/documents") && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleGetDocuments(exchange);
            } else if (path.endsWith("/documents") && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCreateDocument(exchange);
            } else {
                sendError(exchange, 404, "Endpoint non trouvé");
            }
        } catch (Exception e) {
            sendError(exchange, 500, "Erreur interne: " + e.getMessage());
        }
    }

    private void handleGetDocuments(HttpExchange exchange) throws IOException {
        ApiResponse<List<Document>> response = ApiResponse.success("Documents récupérés", documents);
        sendResponse(exchange, 200, JsonUtils.toJson(response));
    }

    private void handleCreateDocument(HttpExchange exchange) throws IOException {
        // Implémentation de la création de document
        // ...
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
        sendResponse(exchange, statusCode, JsonUtils.toJson(errorResponse));
    }

    private void configureCORS(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}