package com.pds.pfe.models;

public class AnalysisRequest {
    private String text;
    private Integer topK;
    private String algorithm;

    // Constructors
    public AnalysisRequest() {}

    public AnalysisRequest(String text) {
        this.text = text;
        this.topK = 5;
    }

    // Getters and Setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
}