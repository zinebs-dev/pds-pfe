package com.pds.pfe.handlers;

import com.pds.pfe.models.ApiResponse;
import com.pds.pfe.services.FileUploadService;
import com.pds.pfe.utils.Constants;
import com.pds.pfe.utils.JsonUtils;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.util.Date;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class FileUploadHandler implements HttpHandler {
    private final FileUploadService fileUploadService;

    public FileUploadHandler() {
        this.fileUploadService = new FileUploadService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        configureCORS(exchange);

        try {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleFileUpload(exchange);
            } else if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleOptions(exchange);
            } else {
                sendError(exchange, 405, "Méthode non autorisée");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Erreur lors de l'upload: " + e.getMessage());
        }
    }

    private void handleFileUpload(HttpExchange exchange) throws IOException {
        System.out.println("=== BACKEND: REQUÊTE UPLOAD REÇUE ===");
        System.out.println("Time: " + new Date());
        System.out.println("Remote address: " + exchange.getRemoteAddress());
        System.out.println("Content-Type: " + exchange.getRequestHeaders().getFirst("Content-Type"));

        long contentLength = 0;
        try {
            contentLength = Long.parseLong(exchange.getRequestHeaders().getFirst("Content-Length"));
            System.out.println("Content-Length: " + contentLength);
        } catch (Exception e) {
            System.err.println("Erreur parsing Content-Length: " + e.getMessage());
        }

        try {
            InputStream requestBody = exchange.getRequestBody();
            byte[] allBytes = requestBody.readAllBytes();
            System.out.println("Bytes reçus: " + allBytes.length);

            if (allBytes.length == 0) {
                System.err.println("Aucun byte reçu!");
                sendError(exchange, 400, "Aucun fichier fourni");
                return;
            }

            // Créer le répertoire d'upload s'il n'existe pas
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                System.out.println("Créé répertoire: " + uploadDir.toAbsolutePath());
            }

            // Générer un nom de fichier unique
            String fileName = "upload_" + System.currentTimeMillis() + ".pdf";
            Path filePath = uploadDir.resolve(fileName);

            System.out.println("Sauvegarde vers: " + filePath.toAbsolutePath());

            // Sauvegarder le fichier
            Files.write(filePath, allBytes);

            System.out.println("Fichier sauvegardé: " + filePath.getFileName());
            System.out.println("Taille fichier: " + Files.size(filePath) + " bytes");

            // Préparer la réponse
            Map<String, Object> result = new HashMap<>();
            result.put("filePath", filePath.toString());
            result.put("fileName", fileName);
            result.put("message", "Fichier uploadé avec succès");
            result.put("size", Files.size(filePath));
            result.put("success", true);

            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success("Upload réussi", result);
            sendResponse(exchange, 200, JsonUtils.toJson(apiResponse));

            System.out.println("=== BACKEND: UPLOAD RÉUSSI ===");

        } catch (Exception e) {
            System.err.println("=== BACKEND: ERREUR UPLOAD ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            sendError(exchange, 500, "Erreur lors de l'upload: " + e.getMessage());
        }
    }

    private void handleOptions(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(200, -1);
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
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}