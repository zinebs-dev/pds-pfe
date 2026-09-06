package com.pds.pfe.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pds.pfe.models.ApiResponse;
import com.pds.pfe.models.Document;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SimilarityService {
    private static final String API_BASE_URL = "http://localhost:5000/api";
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public SimilarityService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public ApiResponse<List<SimilarityResult>> checkSimilarity(String text, int topK) {
        try {
            String requestBody = objectMapper.writeValueAsString(new SimilarityRequest(text, topK));

            Request request = new Request.Builder()
                    .url(API_BASE_URL + "/similarity/check")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    return objectMapper.readValue(responseBody,
                            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class,
                                    objectMapper.getTypeFactory().constructParametricType(List.class, SimilarityResult.class)));
                } else {
                    return ApiResponse.error("Erreur API: " + response.code());
                }
            }
        } catch (IOException e) {
            return ApiResponse.error("Erreur de connexion: " + e.getMessage());
        }
    }

    public ApiResponse<SystemStatus> getSystemStatus() {
        try {
            Request request = new Request.Builder()
                    .url(API_BASE_URL + "/system/status")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    return objectMapper.readValue(responseBody,
                            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, SystemStatus.class));
                } else {
                    return ApiResponse.error("Erreur API: " + response.code());
                }
            }
        } catch (IOException e) {
            return ApiResponse.error("Erreur de connexion: " + e.getMessage());
        }
    }

    public ApiResponse<String> initializeSystem() {
        try {
            Request request = new Request.Builder()
                    .url(API_BASE_URL + "/system/initialize")
                    .post(RequestBody.create("", MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    return objectMapper.readValue(responseBody,
                            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, String.class));
                } else {
                    return ApiResponse.error("Erreur API: " + response.code());
                }
            }
        } catch (IOException e) {
            return ApiResponse.error("Erreur de connexion: " + e.getMessage());
        }
    }

    // Classes internes pour la sérialisation
    private static class SimilarityRequest {
        public String text;
        public int top_k;

        public SimilarityRequest(String text, int top_k) {
            this.text = text;
            this.top_k = top_k;
        }
    }

    public static class SystemStatus {
        public boolean elasticsearch_connected;
        public boolean similarity_system_ready;
        public int total_documents;
        public double timestamp;
    }

    public static class SimilarityResult {
        public String id;
        public double similarity_score;
        public String title;
        public String author;
        public String specialty;
        public String abstractText;
        public String pdf_url;
        public String risk_level;

        // Getters pour JavaFX
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getSpecialty() { return specialty; }
        public double getSimilarityScore() { return similarity_score; }
        public String getRiskLevel() { return risk_level; }
        public String getAbstractText() { return abstractText; }
    }
}