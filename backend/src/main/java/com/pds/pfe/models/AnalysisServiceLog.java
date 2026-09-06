package com.pds.pfe.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

public class AnalysisServiceLog {
    private String logType = "SERVICE";
    private String userId;
    private String username;
    private String action;
    private String pdfName;
    private String pdfId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date timestamp;

    private String requestId;
    private double maxSimilarityScore;
    private int numberOfSourcesFound;
    private long processingTimeMs;
    private String algorithmUsed;
    private int httpStatus;

    public AnalysisServiceLog() {}

    public AnalysisServiceLog(String userId, String username, String action,
                              String pdfName, String pdfId, String requestId) {
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.pdfName = pdfName;
        this.pdfId = pdfId;
        this.timestamp = new Date();
        this.requestId = requestId;
    }

    // Getters et Setters
    public String getLogType() { return logType; }
    public void setLogType(String logType) { this.logType = logType; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getPdfName() { return pdfName; }
    public void setPdfName(String pdfName) { this.pdfName = pdfName; }

    public String getPdfId() { return pdfId; }
    public void setPdfId(String pdfId) { this.pdfId = pdfId; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public double getMaxSimilarityScore() { return maxSimilarityScore; }
    public void setMaxSimilarityScore(double maxSimilarityScore) {
        this.maxSimilarityScore = maxSimilarityScore;
    }

    public int getNumberOfSourcesFound() { return numberOfSourcesFound; }
    public void setNumberOfSourcesFound(int numberOfSourcesFound) {
        this.numberOfSourcesFound = numberOfSourcesFound;
    }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public String getAlgorithmUsed() { return algorithmUsed; }
    public void setAlgorithmUsed(String algorithmUsed) {
        this.algorithmUsed = algorithmUsed;
    }

    public int getHttpStatus() { return httpStatus; }
    public void setHttpStatus(int httpStatus) { this.httpStatus = httpStatus; }
}