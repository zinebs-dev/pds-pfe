package com.pds.pfe.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pds.pfe.models.Document;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ElasticsearchDAO {
    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;
    private final String INDEX_NAME = "documents";

    public ElasticsearchDAO(RestHighLevelClient client) {
        this.client = client;
        this.objectMapper = new ObjectMapper();
    }

    public List<Document> searchDocuments(String query) throws IOException {
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        sourceBuilder.query(QueryBuilders.multiMatchQuery(query,
                "title", "abstract", "general_introduction", "chapters.contenu"));
        sourceBuilder.size(50);

        searchRequest.source(sourceBuilder);

        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        return parseSearchResponse(response);
    }

    public Document getDocumentById(String id) throws IOException {
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        sourceBuilder.query(QueryBuilders.idsQuery().addIds(id));
        searchRequest.source(sourceBuilder);

        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        List<Document> documents = parseSearchResponse(response);

        return documents.isEmpty() ? null : documents.get(0);
    }

    public List<Document> getAllDocuments() throws IOException {
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        sourceBuilder.query(QueryBuilders.matchAllQuery());
        sourceBuilder.size(1000); // Augmenter selon les besoins

        searchRequest.source(sourceBuilder);

        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        return parseSearchResponse(response);
    }

    private List<Document> parseSearchResponse(SearchResponse response) {
        List<Document> documents = new ArrayList<>();

        for (SearchHit hit : response.getHits().getHits()) {
            try {
                Document document = objectMapper.readValue(hit.getSourceAsString(), Document.class);
                document.setId(hit.getId());
                documents.add(document);
            } catch (Exception e) {
                System.err.println("Erreur lors du parsing du document: " + e.getMessage());
            }
        }

        return documents;
    }

    public boolean isConnected() {
        try {
            return client.ping(RequestOptions.DEFAULT);
        } catch (IOException e) {
            return false;
        }
    }
}