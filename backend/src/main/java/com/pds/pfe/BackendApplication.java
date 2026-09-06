package com.pds.pfe;

import com.pds.pfe.handlers.*;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class BackendApplication {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Enregistrement des handlers
        server.createContext("/api/auth", new AuthHandler());
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/documents", new DocumentHandler());
        server.createContext("/api/upload", new FileUploadHandler());
        server.createContext("/api/similarity", new SimilarityHandler());
        server.createContext("/api/history", new HistoryHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("Serveur démarré sur le port " + port);
    }
}