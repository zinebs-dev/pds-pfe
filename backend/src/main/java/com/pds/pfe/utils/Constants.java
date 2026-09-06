package com.pds.pfe.utils;

public class Constants {

    // Configuration Elasticsearch
    public static final String ELASTICSEARCH_HOST = "localhost";
    public static final int ELASTICSEARCH_PORT = 9200;
    public static final String DOCUMENTS_INDEX = "documents";
    public static final String SIMILARITY_INDEX = "similarity_results";
    public static final String USERS_INDEX = "users";

    // Configuration API Python
    public static final String PYTHON_API_BASE_URL = "http://localhost:5000";

    // Chemins des fichiers
    public static final String DATA_DIR = "data";
    public static final String UPLOAD_DIRECTORY = DATA_DIR + "/uploads";
    public static final String EMBEDDINGS_DIR = DATA_DIR + "/embeddings";
    public static final String TEXT_DIRECTORY  = DATA_DIR + "/texts";

    // Limites
    public static final int MAX_FILE_SIZE_MB = 50;
    public static final int MIN_TEXT_LENGTH = 50;
    public static final int DEFAULT_TOP_K = 20;

    // Messages
    public static final String SYSTEM_READY = "Système prêt";
    public static final String SYSTEM_INITIALIZING = "Initialisation du système...";
    public static final String ELASTICSEARCH_DISCONNECTED = "Elasticsearch déconnecté";
}