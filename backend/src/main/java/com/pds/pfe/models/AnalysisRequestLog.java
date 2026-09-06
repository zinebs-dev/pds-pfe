package com.pds.pfe.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

public class AnalysisRequestLog {
    private String logType = "REQUEST";
    private String userId;
    private String username;
    private String action;
    private String pdfName;
    private String pdfId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date timestamp;

    private String requestId;
    private String clientIp;

    public AnalysisRequestLog() {}

    public AnalysisRequestLog(String userId, String username, String action,
                              String pdfName, String pdfId) {
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.pdfName = pdfName;
        this.pdfId = pdfId;
        this.timestamp = new Date();
        this.requestId = java.util.UUID.randomUUID().toString();
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

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
}