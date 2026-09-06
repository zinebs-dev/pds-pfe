package com.pds.pfe.services;

import com.pds.pfe.dao.ElasticsearchDAO;
import com.pds.pfe.models.Document;

import java.io.IOException;
import java.util.List;

public class DocumentService {
    private final ElasticsearchDAO elasticsearchDAO;

    public DocumentService(ElasticsearchDAO elasticsearchDAO) {
        this.elasticsearchDAO = elasticsearchDAO;
    }

    public List<Document> searchDocuments(String query) {
        try {
            return elasticsearchDAO.searchDocuments(query);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la recherche de documents", e);
        }
    }

    public Document getDocumentById(String id) {
        try {
            return elasticsearchDAO.getDocumentById(id);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la récupération du document", e);
        }
    }

    public List<Document> getAllDocuments() {
        try {
            return elasticsearchDAO.getAllDocuments();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la récupération des documents", e);
        }
    }

    public boolean isElasticsearchConnected() {
        return elasticsearchDAO.isConnected();
    }
}