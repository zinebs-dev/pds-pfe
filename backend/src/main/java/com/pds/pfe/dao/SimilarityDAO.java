package com.pds.pfe.dao;

import com.pds.pfe.models.SimilarityResult;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SimilarityDAO {
    private final RestHighLevelClient client;
    private final String INDEX_NAME = "similarity_results";

    public SimilarityDAO(RestHighLevelClient client) {
        this.client = client;
    }

    public void saveSimilarityResult(SimilarityResult result) throws IOException {
        result.setId(UUID.randomUUID().toString());
        result.setComparisonDate(LocalDateTime.now());

        IndexRequest request = new IndexRequest(INDEX_NAME);
        request.id(result.getId());
        request.source(convertToJson(result), XContentType.JSON);

        client.index(request, RequestOptions.DEFAULT);
    }

    public List<SimilarityResult> getSimilarityResultsForDocument(String documentId) throws IOException {
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        sourceBuilder.query(QueryBuilders.boolQuery()
                .should(QueryBuilders.termQuery("sourceDocumentId", documentId))
                .should(QueryBuilders.termQuery("targetDocumentId", documentId)));
        sourceBuilder.size(100);

        searchRequest.source(sourceBuilder);

        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        return parseSearchResponse(response);
    }

    private List<SimilarityResult> parseSearchResponse(SearchResponse response) {
        List<SimilarityResult> results = new ArrayList<>();

        for (SearchHit hit : response.getHits().getHits()) {
            try {
                SimilarityResult result = new SimilarityResult();
                // Implémenter la désérialisation selon votre structure
                results.add(result);
            } catch (Exception e) {
                System.err.println("Erreur lors du parsing du résultat: " + e.getMessage());
            }
        }

        return results;
    }

    private String convertToJson(SimilarityResult result) {
        // Implémenter la sérialisation JSON
        return "{}"; // Placeholder
    }
}