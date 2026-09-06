// backend/src/main/java/com/pds/pfe/services/ElasticsearchLoggerService.java
package com.pds.pfe.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pds.pfe.models.AnalysisRequestLog;
import com.pds.pfe.models.AnalysisServiceLog;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Date;

public class ElasticsearchLoggerService {
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchLoggerService.class);

    // URLs pour les 3 types de logs
    private static final String ELASTICSEARCH_URL = "http://localhost:9200";
    private static final String REQUEST_INDEX = "pfe-logs-request";
    private static final String SERVICE_INDEX = "pfe-logs-service";
    private static final String RESPONSE_INDEX = "pfe-logs-response";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    // Singleton
    private static ElasticsearchLoggerService instance;

    public static synchronized ElasticsearchLoggerService getInstance() {
        if (instance == null) {
            instance = new ElasticsearchLoggerService();
        }
        return instance;
    }

    private ElasticsearchLoggerService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.executorService = Executors.newFixedThreadPool(4);

        // Créer les indexes au démarrage
        createIndexesIfNotExist();
    }

    /**
     * Journalise une requête (Handler level)
     */
    public void logRequestAsync(AnalysisRequestLog requestLog) {
        executorService.submit(() -> {
            try {
                sendToElasticsearch(requestLog, REQUEST_INDEX);
                logger.debug("Log REQUEST envoyé: {}", requestLog.getRequestId());
            } catch (Exception e) {
                logger.error("Erreur log REQUEST: {}", e.getMessage());
                saveToLocalFile(requestLog, "request");
            }
        });
    }

    /**
     * Journalise un traitement service (Service level)
     */
    public void logServiceAsync(AnalysisServiceLog serviceLog) {
        executorService.submit(() -> {
            try {
                sendToElasticsearch(serviceLog, SERVICE_INDEX);
                logger.debug("Log SERVICE envoyé: {}", serviceLog.getRequestId());
            } catch (Exception e) {
                logger.error("Erreur log SERVICE: {}", e.getMessage());
                saveToLocalFile(serviceLog, "service");
            }
        });
    }

    /**
     * Journalise une réponse (Handler level - optionnel)
     */
    public void logResponseAsync(String requestId, String userId, String action,
                                 int httpStatus, String errorMessage) {
        executorService.submit(() -> {
            try {
                // Créer un objet simple pour la réponse
                String responseLog = String.format(
                        "{\"logType\":\"RESPONSE\",\"requestId\":\"%s\",\"userId\":\"%s\"," +
                                "\"action\":\"%s\",\"timestamp\":\"%s\",\"httpStatus\":%d,\"error\":\"%s\"}",
                        requestId, userId, action,
                        new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date()),
                        httpStatus, (errorMessage != null ? errorMessage : "")
                );

                sendJsonToElasticsearch(responseLog, RESPONSE_INDEX);
                logger.debug("Log RESPONSE envoyé pour requestId: {}", requestId);
            } catch (Exception e) {
                logger.error("Erreur log RESPONSE: {}", e.getMessage());
            }
        });
    }

    /**
     * Envoie un objet à Elasticsearch
     */
    private void sendToElasticsearch(Object logObject, String indexName) throws IOException {
        String json = objectMapper.writeValueAsString(logObject);
        sendJsonToElasticsearch(json, indexName);
    }

    private void sendJsonToElasticsearch(String json, String indexName) throws IOException {
        String url = ELASTICSEARCH_URL + "/" + indexName + "/_doc";

        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + response.body().string());
            }
        }
    }

    /**
     * Crée les indexes avec mapping
     */
    private void createIndexesIfNotExist() {
        createIndexIfNotExists(REQUEST_INDEX, getRequestIndexMapping());
        createIndexIfNotExists(SERVICE_INDEX, getServiceIndexMapping());
        createIndexIfNotExists(RESPONSE_INDEX, getResponseIndexMapping());
    }

    private void createIndexIfNotExists(String indexName, String mapping) {
        executorService.submit(() -> {
            try {
                // Vérifier si l'index existe
                Request checkRequest = new Request.Builder()
                        .url(ELASTICSEARCH_URL + "/" + indexName)
                        .head()
                        .build();

                try (Response response = httpClient.newCall(checkRequest).execute()) {
                    if (response.code() == 404) {
                        // Créer l'index
                        RequestBody body = RequestBody.create(
                                mapping,
                                MediaType.parse("application/json")
                        );

                        Request createRequest = new Request.Builder()
                                .url(ELASTICSEARCH_URL + "/" + indexName)
                                .put(body)
                                .build();

                        try (Response createResponse = httpClient.newCall(createRequest).execute()) {
                            if (createResponse.isSuccessful()) {
                                logger.info("Index {} créé avec succès", indexName);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Impossible de créer l'index {}: {}", indexName, e.getMessage());
            }
        });
    }

    private String getRequestIndexMapping() {
        return """
            {
              "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0
              },
              "mappings": {
                "properties": {
                  "logType": { "type": "keyword" },
                  "userId": { "type": "keyword" },
                  "username": { "type": "keyword" },
                  "action": { "type": "keyword" },
                  "pdfName": { "type": "text" },
                  "pdfId": { "type": "keyword" },
                  "timestamp": { "type": "date" },
                  "requestId": { "type": "keyword" },
                  "clientIp": { "type": "ip" }
                }
              }
            }
            """;
    }

    private String getServiceIndexMapping() {
        return """
            {
              "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0
              },
              "mappings": {
                "properties": {
                  "logType": { "type": "keyword" },
                  "userId": { "type": "keyword" },
                  "username": { "type": "keyword" },
                  "action": { "type": "keyword" },
                  "pdfName": { "type": "text" },
                  "pdfId": { "type": "keyword" },
                  "timestamp": { "type": "date" },
                  "requestId": { "type": "keyword" },
                  "maxSimilarityScore": { "type": "float" },
                  "numberOfSourcesFound": { "type": "integer" },
                  "processingTimeMs": { "type": "long" },
                  "algorithmUsed": { "type": "keyword" }
                }
              }
            }
            """;
    }

    private String getResponseIndexMapping() {
        return """
            {
              "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0
              },
              "mappings": {
                "properties": {
                  "logType": { "type": "keyword" },
                  "requestId": { "type": "keyword" },
                  "userId": { "type": "keyword" },
                  "action": { "type": "keyword" },
                  "timestamp": { "type": "date" },
                  "httpStatus": { "type": "integer" },
                  "error": { "type": "text" }
                }
              }
            }
            """;
    }

    /**
     * Sauvegarde locale en cas d'échec
     */
    private void saveToLocalFile(Object logObject, String type) {
        try {
            String logLine = objectMapper.writeValueAsString(logObject);
            java.nio.file.Path logDir = java.nio.file.Paths.get("logs");
            if (!java.nio.file.Files.exists(logDir)) {
                java.nio.file.Files.createDirectories(logDir);
            }

            java.nio.file.Files.write(
                    logDir.resolve(type + "_backup.jsonl"),
                    (logLine + System.lineSeparator()).getBytes(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            logger.error("Erreur sauvegarde locale: {}", e.getMessage());
        }
    }

    /**
     * Arrêt propre
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            logger.info("ElasticsearchLoggerService arrêté");
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}