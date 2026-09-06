package com.pds.pfe.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import com.pds.pfe.models.AnalysisServiceLog;
import com.pds.pfe.models.SimilarityResult;
import java.util.List;
import java.util.Map;

public class PythonSimilarityService {
    private static final String PYTHON_API_URL = "http://localhost:5000/api";
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final ElasticsearchLoggerService loggerService;

    public PythonSimilarityService() {
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.loggerService = ElasticsearchLoggerService.getInstance();
    }
    public Map<String, Object> analyzeText(String text, int topK,
                                           String userId, String username,
                                           String requestId) throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            // Créer la requête
            Map<String, Object> requestBody = Map.of(
                    "text", text,
                    "top_k", topK,
                    "user_id", userId,
                    "request_id", requestId
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            Request request = new Request.Builder()
                    .url(PYTHON_API_URL + "/analyze")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);

                    long processingTime = System.currentTimeMillis() - startTime;

                    // Log SERVICE
                    logServiceResult(result, userId, username, requestId, processingTime);

                    return result;
                } else {
                    throw new Exception("Erreur API Python: " + response.code());
                }
            }
        } catch (Exception e) {
            // Log SERVICE ERROR
            logServiceError(userId, username, requestId, e.getMessage());
            throw e;
        }
    }

    private void logServiceResult(Map<String, Object> result, String userId,
                                  String username, String requestId, long processingTime) {
        try {
            // Extraire les métriques du résultat
            double maxScore = 0.0;
            int sourcesFound = 0;

            if (result.containsKey("similarities")) {
                List<Map<String, Object>> similarities = (List<Map<String, Object>>) result.get("similarities");
                sourcesFound = similarities.size();

                for (Map<String, Object> sim : similarities) {
                    if (sim.containsKey("score")) {
                        double score = ((Number) sim.get("score")).doubleValue();
                        if (score > maxScore) maxScore = score;
                    }
                }
            }

            AnalysisServiceLog serviceLog = new AnalysisServiceLog(
                    userId,
                    username,
                    "PYTHON_ANALYSIS",
                    null, // Pas de pdfName spécifique
                    null, // Pas de pdfId spécifique
                    requestId
            );

            serviceLog.setMaxSimilarityScore(maxScore);
            serviceLog.setNumberOfSourcesFound(sourcesFound);
            serviceLog.setProcessingTimeMs(processingTime);
            serviceLog.setAlgorithmUsed("BM25_MiniLM_Hybrid");

            loggerService.logServiceAsync(serviceLog);

        } catch (Exception e) {
            System.err.println("Erreur lors de la journalisation SERVICE: " + e.getMessage());
        }
    }

    private void logServiceError(String userId, String username, String requestId, String error) {
        AnalysisServiceLog serviceLog = new AnalysisServiceLog(
                userId,
                username,
                "PYTHON_ANALYSIS_ERROR",
                null,
                null,
                requestId
        );

        serviceLog.setMaxSimilarityScore(0.0);
        serviceLog.setNumberOfSourcesFound(0);
        serviceLog.setProcessingTimeMs(0);
        serviceLog.setAlgorithmUsed("ERROR");

        loggerService.logServiceAsync(serviceLog);
    }

    // Méthode existante pour compatibilité
    public Map<String, Object> analyzeText(String text, int topK) throws Exception {
        return analyzeText(text, topK, "unknown", "anonymous", null);
    }
}