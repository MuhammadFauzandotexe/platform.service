package mdro.platform.service.integration.qdrant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mdro.platform.service.integration.qdrant.config.QdrantProperties;
import mdro.platform.service.integration.qdrant.exception.QdrantException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class QdrantServiceImplTest {

    private static final UUID POINT_ID = UUID.fromString("e5548aca-ac3f-4f64-a942-d088f005e7f8");

    private HttpServer server;
    private QdrantServiceImpl service;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        QdrantProperties properties = new QdrantProperties();
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.setCollectionName("knowledge");
        properties.setVectorSize(4096);
        properties.setTimeout(Duration.ofSeconds(5));
        service = new QdrantServiceImpl(
                WebClient.builder().baseUrl(properties.getBaseUrl()).build(),
                properties);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void upsertsValidVectorAndBuildsExpectedRequest() {
        server.createContext("/collections/knowledge/points", exchange -> {
            assertEquals("PUT", exchange.getRequestMethod());
            String body = readBody(exchange);
            assertEquals(4096, countOccurrences(body, "0.1"));
            org.junit.jupiter.api.Assertions.assertTrue(body.contains(
                    "\"id\":\"" + POINT_ID + "\""));
            org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"tenantId\":\"tenant-uuid\""));
            respond(exchange, 200, """
                    {"result":{"status":"acknowledged"},"status":"ok","time":0.001}
                    """);
        });
        server.start();

        assertDoesNotThrow(() -> service.upsertVector(
                POINT_ID,
                vectorOfSize(4096),
                Map.of("tenantId", "tenant-uuid", "content", "example")));
    }

    @Test
    void rejectsInvalidVectorDimensionBeforeCallingQdrant() {
        server.createContext("/collections/knowledge/points", exchange -> {
            throw new AssertionError("Qdrant must not be called for an invalid vector");
        });
        server.start();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.upsertVector(
                        POINT_ID,
                        vectorOfSize(3),
                        Map.of("tenantId", "tenant-uuid")));

        assertEquals("Expected vector dimension: 4096, Actual vector dimension: 3",
                exception.getMessage());
    }

    @Test
    void rejectsPayloadWithoutTenantIdBeforeCallingQdrant() {
        server.createContext("/collections/knowledge/points", exchange -> {
            throw new AssertionError("Qdrant must not be called without tenantId");
        });
        server.start();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.upsertVector(
                        POINT_ID,
                        vectorOfSize(4096),
                        Map.of("content", "example")));

        assertEquals("tenantId is required in Qdrant payload", exception.getMessage());
    }

    @Test
    void mapsQdrantHttpErrorToQdrantException() {
        server.createContext("/collections/knowledge/points", exchange -> respond(exchange, 500, """
                {"status":"error","result":"collection unavailable"}
                """));
        server.start();

        QdrantException exception = assertThrows(
                QdrantException.class,
                () -> service.upsertVector(
                        POINT_ID,
                        vectorOfSize(4096),
                        Map.of("tenantId", "tenant-uuid")));

        assertEquals(500, exception.getHttpStatusCode());
    }

    private static List<Float> vectorOfSize(int size) {
        return java.util.stream.IntStream.range(0, size)
                .mapToObj(index -> 0.1f)
                .toList();
    }

    private static int countOccurrences(String value, String search) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(search, index)) >= 0) {
            count++;
            index += search.length();
        }
        return count;
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
