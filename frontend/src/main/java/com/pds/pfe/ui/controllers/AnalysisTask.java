package com.pds.pfe.ui.controllers;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Représente une session d'analyse (comme un "chat" dans ChatGPT)
 * Stocke les données d'une analyse, mais n'est PAS un Thread
 */
public class AnalysisTask {
    private String id;
    private String userId;
    private String username;
    private String filename;
    private File file;
    private String status;
    private double similarityScore;
    private Map<String, Object> fullResults;
    private LocalDateTime createdTime;
    private LocalDateTime completedTime;
    private long fileSize;
    private long analysisDurationMs;
    private String documentReference; // Référence au document dans ES

    // Formateur pour les dates
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Constructeurs
    public AnalysisTask() {
        this.id = generateId();
        this.createdTime = LocalDateTime.now();
        this.status = "IDLE";
    }

    public AnalysisTask(String userId, String username) {
        this();
        this.userId = userId;
        this.username = username;
    }

    public AnalysisTask(String userId, String username, String filename) {
        this(userId, username);
        this.filename = filename;
    }

    // Génère un ID unique
    private String generateId() {
        return "analysis_" + System.currentTimeMillis() + "_" +
                (int)(Math.random() * 1000);
    }

    // === GETTERS ET SETTERS ===

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        if (file != null) {
            this.fileSize = file.length();
            if (this.filename == null) {
                this.filename = file.getName();
            }
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            this.completedTime = LocalDateTime.now();
            if (this.completedTime != null && this.createdTime != null) {
                this.analysisDurationMs = java.time.Duration.between(
                        createdTime, completedTime
                ).toMillis();
            }
        }
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public Map<String, Object> getFullResults() {
        return fullResults;
    }

    public void setFullResults(Map<String, Object> fullResults) {
        this.fullResults = fullResults;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getAnalysisDurationMs() {
        return analysisDurationMs;
    }

    public void setAnalysisDurationMs(long analysisDurationMs) {
        this.analysisDurationMs = analysisDurationMs;
    }

    public String getDocumentReference() {
        return documentReference;
    }

    public void setDocumentReference(String documentReference) {
        this.documentReference = documentReference;
    }

    // === MÉTHODES UTILITAIRES ===

    public String getFormattedDate() {
        if (createdTime != null) {
            return createdTime.format(DATE_FORMATTER);
        }
        return "";
    }

    public String getFormattedCompletedDate() {
        if (completedTime != null) {
            return completedTime.format(DATE_FORMATTER);
        }
        return "";
    }

    public String getStatusIcon() {
        switch(status) {
            case "RUNNING": return "⏳";
            case "COMPLETED": return "✅";
            case "FAILED": return "❌";
            default: return "📄";
        }
    }

    public String getDisplayName() {
        if (filename != null && !filename.isEmpty()) {
            return filename + " " + getStatusIcon();
        }
        return "Nouvelle analyse " + getStatusIcon();
    }

    public String getFormattedFileSize() {
        if (fileSize < 1024) return fileSize + " B";
        int exp = (int) (Math.log(fileSize) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", fileSize / Math.pow(1024, exp), pre);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isRunning() {
        return "RUNNING".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    public boolean isIdle() {
        return "IDLE".equals(status);
    }

    @Override
    public String toString() {
        return "AnalysisTask{" +
                "id='" + id + '\'' +
                ", filename='" + filename + '\'' +
                ", status='" + status + '\'' +
                ", similarity=" + similarityScore +
                ", created=" + getFormattedDate() +
                '}';
    }


}