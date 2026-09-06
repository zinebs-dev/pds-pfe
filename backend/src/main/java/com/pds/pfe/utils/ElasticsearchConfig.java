package com.pds.pfe.utils;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public class ElasticsearchConfig {
    private static RestHighLevelClient client;

    public static RestHighLevelClient getClient() {
        if (client == null) {
            client = new RestHighLevelClient(
                    RestClient.builder(
                            new HttpHost("localhost", 9200, "http")
                    )
            );
        }
        return client;
    }

    public static void closeClient() {
        try {
            if (client != null) {
                client.close();
                client = null;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la fermeture du client Elasticsearch: " + e.getMessage());
        }
    }
}