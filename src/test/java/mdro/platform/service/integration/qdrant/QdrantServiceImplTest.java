package mdro.platform.service.integration.qdrant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import mdro.platform.service.integration.qdrant.exception.QdrantIntegrationException;
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

    @Test
    void searchesWithTenantFilterAndMapsResponse() {
        UUID tenantId = UUID.fromString("8a88b11b-140d-4cb3-bf62-e51eab7c4262");
        UUID pointId = UUID.fromString("ee3ddd6f-2eac-4781-9d7b-1353ad77430b");
        UUID documentId = UUID.fromString("f652bbf9-c572-4ea7-9fae-ecb5ef0b40c6");
        UUID chunkId = UUID.fromString("842c19aa-804f-4c6c-9227-47ed92fccd28");
        server.createContext("/collections/knowledge/points/query", exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            String body = readBody(exchange);
            assertTrue(body.contains("\"limit\":3"));
            assertTrue(body.contains("\"with_payload\":true"));
            assertTrue(body.contains("\"score_threshold\":0.6"));
            assertTrue(body.contains("\"key\":\"tenantId\""));
            assertTrue(body.contains("\"value\":\"" + tenantId + "\""));
            respond(exchange, 200, """
                    {
                      "result": {
                        "points": [{
                          "id": "ee3ddd6f-2eac-4781-9d7b-1353ad77430b",
                          "version": 7,
                          "score": 0.69882184,
                          "payload": {
                            "tenantId": "8a88b11b-140d-4cb3-bf62-e51eab7c4262",
                            "documentId": "f652bbf9-c572-4ea7-9fae-ecb5ef0b40c6",
                            "chunkId": "842c19aa-804f-4c6c-9227-47ed92fccd28",
                            "content": "Produk dapat dikembalikan maksimal 7 hari setelah pembelian.",
                            "chunkIndex": 0,
                            "createdAt": "2026-09-06T08:01:04.299649600Z"
                          }
                        }]
                      },
                      "status": "ok",
                      "time": 0.000597504
                    }
                    """);
        });
        server.start();

        List<QdrantSearchResult> results = service.search(
                vectorOfSize(4096), tenantId, 3, 0.6);

        assertEquals(1, results.size());
        assertEquals(new QdrantSearchResult(
                pointId,
                0.69882184,
                documentId,
                chunkId,
                "Produk dapat dikembalikan maksimal 7 hari setelah pembelian.",
                0), results.get(0));
    }

    @Test
    void returnsEmptyListWhenQdrantReturnsNoPoints() {
        server.createContext("/collections/knowledge/points/query", exchange -> respond(exchange, 200, """
                {"result":{"points":[]},"status":"ok","time":0.001}
                """));
        server.start();

        assertEquals(List.of(), service.search(
                vectorOfSize(4096),
                UUID.randomUUID(),
                null,
                null));
    }

    @Test
    void rejectsInvalidSearchVectorDimension() {
        server.start();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.search(
                        vectorOfSize(3),
                        UUID.randomUUID(),
                        3,
                        0.6));

        assertEquals("Expected vector dimension: 4096, Actual vector dimension: 3",
                exception.getMessage());
    }

    @Test
    void mapsSearchHttpErrorToQdrantIntegrationException() {
        server.createContext("/collections/knowledge/points/query", exchange -> respond(exchange, 503, """
                {"status":"error"}
                """));
        server.start();

        QdrantIntegrationException exception = assertThrows(
                QdrantIntegrationException.class,
                () -> service.search(
                        vectorOfSize(4096),
                        UUID.randomUUID(),
                        3,
                        0.6));

        assertEquals(503, exception.getHttpStatusCode());
    }

    @Test
    void scrollsKnowledgeWithTenantFilterWithoutVectorsAndMapsCursor() {
        UUID tenantId = UUID.fromString("8a88b11b-140d-4cb3-bf62-e51eab7c4262");
        server.createContext("/collections/knowledge/points/scroll", exchange -> {
            String body = readBody(exchange);
            assertTrue(body.contains("\"limit\":20"));
            assertTrue(body.contains("\"with_payload\":true"));
            assertTrue(body.contains("\"with_vector\":false"));
            assertTrue(body.contains("\"key\":\"tenantId\""));
            assertTrue(body.contains("\"value\":\"" + tenantId + "\""));
            respond(exchange, 200, """
                    {
                      "result": {
                        "points": [{
                          "id": "ee3ddd6f-2eac-4781-9d7b-1353ad77430b",
                          "payload": {
                            "tenantId": "8a88b11b-140d-4cb3-bf62-e51eab7c4262",
                            "documentId": "f652bbf9-c572-4ea7-9fae-ecb5ef0b40c6",
                            "chunkId": "842c19aa-804f-4c6c-9227-47ed92fccd28",
                            "content": "return policy",
                            "chunkIndex": 0,
                            "createdAt": "2026-09-06T08:01:04.299649600Z"
                          }
                        }],
                        "next_page_offset": "f0f1e2d3-c4b5-4678-9012-345678901234"
                      },
                      "status": "ok",
                      "time": 0.001
                    }
                    """);
        });
        server.start();

        QdrantKnowledgePage page = service.scrollKnowledge(tenantId, 20, null);

        assertEquals(1, page.points().size());
        assertEquals("f0f1e2d3-c4b5-4678-9012-345678901234", page.nextCursor());
        assertEquals("return policy", page.points().get(0).content());
    }

    @Test
    void deletesOnlyWhenPointAndTenantFiltersMatch() {
        UUID tenantId = UUID.fromString("8a88b11b-140d-4cb3-bf62-e51eab7c4262");
        UUID pointId = UUID.fromString("ee3ddd6f-2eac-4781-9d7b-1353ad77430b");
        server.createContext("/collections/knowledge/points/scroll", exchange -> {
            String body = readBody(exchange);
            assertTrue(body.contains("\"has_id\":[\"" + pointId + "\"]"));
            assertTrue(body.contains("\"value\":\"" + tenantId + "\""));
            respond(exchange, 200, """
                    {
                      "result": {
                        "points": [{
                          "id": "ee3ddd6f-2eac-4781-9d7b-1353ad77430b",
                          "payload": {
                            "tenantId": "8a88b11b-140d-4cb3-bf62-e51eab7c4262",
                            "documentId": "f652bbf9-c572-4ea7-9fae-ecb5ef0b40c6",
                            "chunkId": "842c19aa-804f-4c6c-9227-47ed92fccd28"
                          }
                        }]
                      },
                      "status": "ok"
                    }
                    """);
        });
        server.createContext("/collections/knowledge/points/delete", exchange -> {
            String body = readBody(exchange);
            assertTrue(body.contains("\"has_id\":[\"" + pointId + "\"]"));
            assertTrue(body.contains("\"key\":\"tenantId\""));
            respond(exchange, 200, """
                    {"result":{"status":"acknowledged"},"status":"ok"}
                    """);
        });
        server.start();

        assertTrue(service.deleteKnowledgeForTenant(pointId, tenantId));
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
