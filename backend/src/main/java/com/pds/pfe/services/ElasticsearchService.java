// src/main/java/com/pds/pfe/services/ElasticsearchService.java
package com.pds.pfe.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RestClient;
import org.apache.http.HttpHost;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.util.Map;

public class ElasticsearchService {
    private RestHighLevelClient client;
    private ObjectMapper objectMapper;

    public ElasticsearchService() {
        this.client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http"))
        );
        this.objectMapper = new ObjectMapper();
    }

    // Créer un nouvel utilisateur - CORRIGÉ
    public boolean createUser(Map<String, Object> userData) {
        try {
            String userId = (String) userData.get("id");

            IndexRequest request = new IndexRequest("users")
                    .id(userId)
                    .source(userData, XContentType.JSON);

            IndexResponse response = client.index(request, RequestOptions.DEFAULT);

            System.out.println("Elasticsearch response: " + response.status());

            return response.status().getStatus() < 300;
        } catch (IOException e) {
            System.err.println("Erreur lors de la création de l'utilisateur: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Rechercher par email - CORRIGÉ
    public Map<String, Object> getUserByEmail(String email) {
        try {
            SearchRequest request = new SearchRequest("users");
            request.source().query(QueryBuilders.termQuery("email", email.toLowerCase()));
            request.source().size(1);

            SearchResponse response = client.search(request, RequestOptions.DEFAULT);

            if (response.getHits().getTotalHits().value > 0) {
                SearchHit hit = response.getHits().getAt(0);
                return hit.getSourceAsMap();
            }

            return null;
        } catch (IOException e) {
            System.err.println("Erreur lors de la recherche par email: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Rechercher par username - CORRIGÉ
    public Map<String, Object> getUserByUsername(String username) {
        try {
            SearchRequest request = new SearchRequest("users");
            request.source().query(QueryBuilders.termQuery("username", username));
            request.source().size(1);

            SearchResponse response = client.search(request, RequestOptions.DEFAULT);

            if (response.getHits().getTotalHits().value > 0) {
                SearchHit hit = response.getHits().getAt(0);
                return hit.getSourceAsMap();
            }

            return null;
        } catch (IOException e) {
            System.err.println("Erreur lors de la recherche par username: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Fermer la connexion
    public void close() {
        try {
            if (client != null) {
                client.close();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la fermeture Elasticsearch: " + e.getMessage());
        }
    }
}