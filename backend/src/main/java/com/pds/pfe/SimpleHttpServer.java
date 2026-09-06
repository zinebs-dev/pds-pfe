package com.pds.pfe;

import com.pds.pfe.handlers.*;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.pds.pfe.services.ElasticsearchLoggerService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class SimpleHttpServer {
    public static void main(String[] args) throws IOException {
        // Créer le répertoire d'upload s'il n'existe pas
        try {
            Path uploadDir = Paths.get("data/uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                System.out.println("Répertoire créé: " + uploadDir.toAbsolutePath());
            }

            // Créer le répertoire logs pour la sauvegarde
            Path logDir = Paths.get("logs");
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
                System.out.println("Répertoire logs créé: " + logDir.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Erreur création répertoires: " + e.getMessage());
        }

        // Initialiser le service de journalisation
        ElasticsearchLoggerService loggerService = ElasticsearchLoggerService.getInstance();
        System.out.println("Service de journalisation initialisé");

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Enregistrement de tous les handlers
        server.createContext("/api/auth", new AuthHandler());
        server.createContext("/api/upload", new FileUploadHandler());
        server.createContext("/api/similarity", new SimilarityHandler());
        server.createContext("/api/documents", new DocumentHandler());
        server.createContext("/api/health", new HealthHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("Serveur PDS-PFE démarré sur http://localhost:8080");
        System.out.println("===================================================");
        System.out.println("Journalisation activée pour:");
        System.out.println("   - pfe-logs-request");
        System.out.println("   - pfe-logs-service");
        System.out.println("   - pfe-logs-response");
        System.out.println("===================================================");

        // Ajouter un shutdown hook pour arrêter proprement
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Arrêt du serveur en cours...");
            loggerService.shutdown();
            System.out.println("Serveur arrêté");
        }));
    }
}