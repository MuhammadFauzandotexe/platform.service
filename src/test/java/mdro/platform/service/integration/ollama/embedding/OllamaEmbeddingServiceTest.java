package mdro.platform.service.integration.ollama.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import mdro.platform.service.integration.ollama.config.OllamaProperties;
import mdro.platform.service.integration.ollama.exception.OllamaEmbeddingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class OllamaEmbeddingServiceTest {

    private HttpServer server;
    private OllamaEmbeddingService service;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        OllamaProperties properties = new OllamaProperties();
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.getEmbedding().setModel("qwen3-embedding");
        properties.getEmbedding().setTimeout(Duration.ofSeconds(5));
        service = new OllamaEmbeddingService(
                WebClient.builder().baseUrl(properties.getBaseUrl()).build(),
                properties);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void returnsFirstEmbeddingFromResponse() {
        server.createContext("/api/embed", exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals(
                    "{\"model\":\"qwen3-embedding\",\"input\":\"test text\"}",
                    readBody(exchange));
            respond(exchange, 200, """
                    {
                      "model": "qwen3-embedding",
                      "embeddings": [[0.1, 0.2, -0.3]],
                      "total_duration": 10,
                      "load_duration": 2,
                      "prompt_eval_count": 2
                    }
                    """);
        });
        server.start();

        assertEquals(List.of(0.1f, 0.2f, -0.3f), service.embed("test text"));
    }

    @Test
    void throwsWhenEmbeddingIsEmpty() {
        server.createContext("/api/embed", exchange -> respond(exchange, 200, """
                {"model":"qwen3-embedding","embeddings":[]}
                """));
        server.start();

        OllamaEmbeddingException exception = assertThrows(
                OllamaEmbeddingException.class,
                () -> service.embed("test text"));

        assertEquals("Ollama returned empty embeddings", exception.getMessage());
    }

    @Test
    void throwsWithHttpStatusWhenOllamaReturnsError() {
        server.createContext("/api/embed", exchange -> respond(exchange, 500, """
                {"error":"model unavailable"}
                """));
        server.start();

        OllamaEmbeddingException exception = assertThrows(
                OllamaEmbeddingException.class,
                () -> service.embed("test text"));

        assertEquals(500, exception.getHttpStatusCode());
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        try (var outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }
}
