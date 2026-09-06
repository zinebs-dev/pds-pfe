package com.pds.pfe.handlers;

import com.pds.pfe.models.ApiResponse;
import com.pds.pfe.services.ElasticsearchService;
import com.pds.pfe.utils.JsonUtils;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import at.favre.lib.crypto.bcrypt.BCrypt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AuthHandler implements HttpHandler {
    private final ElasticsearchService elasticsearchService;

    public AuthHandler() {
        this.elasticsearchService = new ElasticsearchService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        configureCORS(exchange);

        try {
            String path = exchange.getRequestURI().getPath();

            if (path.endsWith("/login")) {
                handleLogin(exchange);
            } else if (path.endsWith("/register")) {
                handleRegister(exchange);
            } else {
                sendError(exchange, 404, "Endpoint non trouvé");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Erreur interne: " + e.getMessage());
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Méthode non autorisée");
            return;
        }

        InputStream requestBody = exchange.getRequestBody();
        String requestText = new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);

        Map<?, ?> credentials = JsonUtils.fromJson(requestText, Map.class);
        String usernameOrEmail = (String) credentials.get("username");
        String password = (String) credentials.get("password");

        if (usernameOrEmail == null || password == null) {
            sendError(exchange, 400, "Username et password sont requis");
            return;
        }

        // Rechercher l'utilisateur
        Map<String, Object> userData = null;

        if (usernameOrEmail.contains("@")) {
            userData = elasticsearchService.getUserByEmail(usernameOrEmail);
        } else {
            userData = elasticsearchService.getUserByUsername(usernameOrEmail);
        }

        if (userData == null) {
            sendError(exchange, 401, "Utilisateur non trouvé");
            return;
        }

        // Vérifier le mot de passe avec BCrypt
        String storedHash = (String) userData.get("password_hash");

        if (storedHash == null) {
            sendError(exchange, 500, "Erreur de configuration du compte");
            return;
        }

        // Vérification BCrypt
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), storedHash);

        if (result.verified) {
            // Authentification réussie
            Map<String, Object> safeUserData = prepareUserResponse(userData);

            Map<String, Object> response = new HashMap<>();
            response.put("token", generateToken((String) userData.get("id")));
            response.put("user", safeUserData);

            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success("Connexion réussie", response);
            sendResponse(exchange, 200, JsonUtils.toJson(apiResponse));
        } else {
            sendError(exchange, 401, "Mot de passe incorrect");
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Méthode non autorisée");
            return;
        }

        InputStream requestBody = exchange.getRequestBody();
        String requestText = new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);

        System.out.println("=== REGISTER REQUEST ===");
        System.out.println("Request body: " + requestText);

        Map<?, ?> requestData = JsonUtils.fromJson(requestText, Map.class);

        String username = (String) requestData.get("username");  // Le username entré par l'utilisateur
        String email = (String) requestData.get("email");
        String password = (String) requestData.get("password");

        // Validation
        if (username == null || username.trim().isEmpty()) {
            sendError(exchange, 400, "Le nom d'utilisateur est requis");
            return;
        }

        if (email == null || password == null) {
            sendError(exchange, 400, "Email et password sont requis");
            return;
        }

        if (password.length() < 6) {
            sendError(exchange, 400, "Le mot de passe doit contenir au moins 6 caractères");
            return;
        }

        // Vérifier si l'email existe déjà
        Map<String, Object> existingUserByEmail = elasticsearchService.getUserByEmail(email);
        if (existingUserByEmail != null) {
            sendError(exchange, 409, "Un utilisateur avec cet email existe déjà");
            return;
        }

        // Vérifier si le username existe déjà
        Map<String, Object> existingUserByUsername = elasticsearchService.getUserByUsername(username);
        if (existingUserByUsername != null) {
            sendError(exchange, 409, "Ce nom d'utilisateur est déjà pris");
            return;
        }

        // Hacher le mot de passe avec BCrypt
        String passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        // Créer l'objet utilisateur
        Map<String, Object> newUser = new HashMap<>();
        String userId = UUID.randomUUID().toString();
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        newUser.put("id", userId);
        newUser.put("username", username.trim());  // Utiliser le username tel quel
        newUser.put("email", email.toLowerCase());
        newUser.put("password_hash", passwordHash);
        newUser.put("created_at", currentTime);
        newUser.put("last_login", currentTime);

        System.out.println("Username: " + username.trim());
        System.out.println("Email: " + email.toLowerCase());

        // Sauvegarder dans Elasticsearch
        System.out.println("Saving user to Elasticsearch...");
        boolean success = elasticsearchService.createUser(newUser);

        if (success) {
            Map<String, Object> responseUser = prepareUserResponse(newUser);

            Map<String, Object> response = new HashMap<>();
            response.put("user", responseUser);
            response.put("message", "Utilisateur créé avec succès");

            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(response);
            sendResponse(exchange, 201, JsonUtils.toJson(apiResponse));
        } else {
            sendError(exchange, 500, "Erreur lors de la création de l'utilisateur");
        }
    }
    private Map<String, Object> prepareUserResponse(Map<String, Object> userData) {
        Map<String, Object> response = new HashMap<>();

        // Copier les champs pertinents (ne pas envoyer le hash du mot de passe)
        if (userData.containsKey("id")) response.put("id", userData.get("id"));
        if (userData.containsKey("email")) response.put("email", userData.get("email"));
        if (userData.containsKey("username")) response.put("username", userData.get("username"));
        if (userData.containsKey("created_at")) response.put("created_at", userData.get("created_at"));
        if (userData.containsKey("last_login")) response.put("last_login", userData.get("last_login"));
        if (userData.containsKey("permissions")) response.put("permissions", userData.get("permissions"));
        // ⚠️ TEMPORAIRE - POUR TEST SEULEMENT
        if (userData.containsKey("password_hash")) {
            response.put("password_hash", userData.get("password_hash"));
        }
        return response;
    }

    private String generateToken(String userId) {
        return "token_" + userId + "_" + System.currentTimeMillis();
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