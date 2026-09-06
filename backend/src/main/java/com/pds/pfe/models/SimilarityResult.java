package com.pds.pfe.models;

import java.time.LocalDateTime;

public class SimilarityResult {
    private String id;
    private String sourceDocumentId;
    private String targetDocumentId;
    private double similarityScore;
    private double contentSimilarity;
    private double semanticSimilarity;
    private double metadataSimilarity;
    private String[] matchedKeywords;
    private String topMatchingTerms;
    private String algorithmUsed;
    private LocalDateTime comparisonDate;

    // Constructeurs
    public SimilarityResult() {}

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSourceDocumentId() { return sourceDocumentId; }
    public void setSourceDocumentId(String sourceDocumentId) { this.sourceDocumentId = sourceDocumentId; }

    public String getTargetDocumentId() { return targetDocumentId; }
    public void setTargetDocumentId(String targetDocumentId) { this.targetDocumentId = targetDocumentId; }

    public double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }

    public double getContentSimilarity() { return contentSimilarity; }
    public void setContentSimilarity(double contentSimilarity) { this.contentSimilarity = contentSimilarity; }

    public double getSemanticSimilarity() { return semanticSimilarity; }
    public void setSemanticSimilarity(double semanticSimilarity) { this.semanticSimilarity = semanticSimilarity; }

    public double getMetadataSimilarity() { return metadataSimilarity; }
    public void setMetadataSimilarity(double metadataSimilarity) { this.metadataSimilarity = metadataSimilarity; }

    public String[] getMatchedKeywords() { return matchedKeywords; }
    public void setMatchedKeywords(String[] matchedKeywords) { this.matchedKeywords = matchedKeywords; }

    public String getTopMatchingTerms() { return topMatchingTerms; }
    public void setTopMatchingTerms(String topMatchingTerms) { this.topMatchingTerms = topMatchingTerms; }

    public String getAlgorithmUsed() { return algorithmUsed; }
    public void setAlgorithmUsed(String algorithmUsed) { this.algorithmUsed = algorithmUsed; }

    public LocalDateTime getComparisonDate() { return comparisonDate; }
    public void setComparisonDate(LocalDateTime comparisonDate) { this.comparisonDate = comparisonDate; }
}