import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

public class JavaBackendTest {

    private static final String PYTHON_API_URL = "http://localhost:5000";
    private static final String JAVA_API_URL = "http://localhost:8080";

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testPythonApiHealth() {
        System.out.println("=== Test santé API Python ===");

        try {
            Request request = new Request.Builder()
                    .url(PYTHON_API_URL + "/api/health")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 200) {
                    String responseBody = response.body().string();
                    System.out.println("✅ API Python fonctionnelle: " + responseBody);
                } else {
                    System.out.println("⚠️ API Python - Code: " + response.code());
                }
            }
        } catch (IOException e) {
            System.out.println("❌ API Python hors ligne: " + e.getMessage());
        }
    }

    @Test
    void testElasticsearchConnection() {
        System.out.println("=== Test connexion Elasticsearch ===");

        try {
            Request request = new Request.Builder()
                    .url("http://localhost:9200")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 200) {
                    System.out.println("✅ Elasticsearch fonctionnel");
                } else {
                    System.out.println("⚠️ Elasticsearch - Code: " + response.code());
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Elasticsearch hors ligne: " + e.getMessage());
        }
    }

    @Test
    void testSimilarityAnalysis() {
        System.out.println("=== Test analyse similarité ===");

        try {
            String requestBody = mapper.writeValueAsString(Map.of(
                    "text", "Test de similarité PFE",
                    "top_k", 2
            ));

            Request request = new Request.Builder()
                    .url(PYTHON_API_URL + "/api/analyze")
                    .post(RequestBody.create(
                            requestBody,
                            MediaType.parse("application/json")
                    ))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 200) {
                    System.out.println("✅ Analyse de similarité réussie");
                } else {
                    System.out.println("⚠️ Analyse échouée - Code: " + response.code());
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Erreur analyse: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   TESTS BACKEND PFE");
        System.out.println("========================================");

        JavaBackendTest tester = new JavaBackendTest();

        tester.testElasticsearchConnection();
        System.out.println();

        tester.testPythonApiHealth();
        System.out.println();

        tester.testSimilarityAnalysis();

        System.out.println("\n========================================");
        System.out.println("   TESTS TERMINÉS");
        System.out.println("========================================");
    }
}