package mdro.platform.service.integration.whatsapp.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import mdro.platform.service.integration.whatsapp.config.WhatsAppGatewayProperties;
import mdro.platform.service.integration.whatsapp.dto.WhatsAppSessionResponse;
import mdro.platform.service.integration.whatsapp.exception.WhatsAppGatewayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class WhatsAppGatewayClientTest {

    private static final UUID TENANT_ID = UUID.fromString("e5548aca-ac3f-4f64-a942-d088f005e7f8");
    private static final UUID SESSION_ID = UUID.fromString("e5548aca-ac3f-4f64-a942-d088f005e7f7");

    private HttpServer server;
    private WhatsAppGatewayClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        WhatsAppGatewayProperties properties = new WhatsAppGatewayProperties();
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        client = new WhatsAppGatewayClient(WebClient.builder(), properties);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void createSessionSendsRequestAndMapsResponse() {
        server.createContext("/sessions", exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals(
                    "{\"tenantId\":\"" + TENANT_ID + "\",\"sessionId\":\"" + SESSION_ID + "\"}",
                    readBody(exchange));
            respond(exchange, 200, """
                    {
                      "tenantId": "e5548aca-ac3f-4f64-a942-d088f005e7f8",
                      "sessionId": "e5548aca-ac3f-4f64-a942-d088f005e7f7",
                      "status": "QR_READY",
                      "qrCode": "qr-value"
                    }
                    """);
        });
        server.start();

        WhatsAppSessionResponse response = client.createSession(TENANT_ID, SESSION_ID);

        assertEquals(TENANT_ID, response.tenantId());
        assertEquals(SESSION_ID, response.sessionId());
        assertEquals("QR_READY", response.status());
        assertEquals("qr-value", response.qrCode());
    }

    @Test
    void getSessionsByTenantMapsNullableQrCode() {
        server.createContext("/sessions/" + TENANT_ID, exchange -> respond(exchange, 200, """
                [
                  {
                    "tenantId": "e5548aca-ac3f-4f64-a942-d088f005e7f8",
                    "sessionId": "e5548aca-ac3f-4f64-a942-d088f005e7f7",
                    "status": "DISCONNECTED"
                  }
                ]
                """));
        server.start();

        List<WhatsAppSessionResponse> responses = client.getSessionsByTenant(TENANT_ID);

        assertEquals(1, responses.size());
        assertEquals("DISCONNECTED", responses.get(0).status());
        assertNull(responses.get(0).qrCode());
    }

    @Test
    void disconnectSessionMapsResponse() {
        server.createContext(
                "/sessions/" + TENANT_ID + "/" + SESSION_ID + "/disconnect",
                exchange -> respond(exchange, 200, """
                        {
                          "tenantId": "e5548aca-ac3f-4f64-a942-d088f005e7f8",
                          "sessionId": "e5548aca-ac3f-4f64-a942-d088f005e7f7",
                          "status": "DISCONNECTED"
                        }
                        """));
        server.start();

        WhatsAppSessionResponse response = client.disconnectSession(TENANT_ID, SESSION_ID);

        assertEquals("DISCONNECTED", response.status());
        assertNull(response.qrCode());
    }

    @Test
    void deleteSessionHandlesNoContent() {
        server.createContext(
                "/sessions/" + TENANT_ID + "/" + SESSION_ID,
                exchange -> {
                    assertEquals("DELETE", exchange.getRequestMethod());
                    respond(exchange, 204, "");
                });
        server.start();

        client.deleteSession(TENANT_ID, SESSION_ID);
    }

    @Test
    void mapsClientErrorAndGatewayErrorCode() {
        server.createContext("/sessions", exchange -> respond(exchange, 409, """
                {"error":"SESSION_ALREADY_EXISTS"}
                """));
        server.start();

        WhatsAppGatewayException exception = assertThrows(
                WhatsAppGatewayException.class,
                () -> client.createSession(TENANT_ID, SESSION_ID));

        assertEquals(409, exception.getHttpStatusCode());
        assertEquals("SESSION_ALREADY_EXISTS", exception.getGatewayError());
    }

    @Test
    void mapsServerErrorAndGatewayErrorCode() {
        server.createContext("/sessions/" + TENANT_ID, exchange -> respond(exchange, 500, """
                {"error":"GATEWAY_UNAVAILABLE"}
                """));
        server.start();

        WhatsAppGatewayException exception = assertThrows(
                WhatsAppGatewayException.class,
                () -> client.getSessionsByTenant(TENANT_ID));

        assertEquals(500, exception.getHttpStatusCode());
        assertEquals("GATEWAY_UNAVAILABLE", exception.getGatewayError());
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        if (response.length > 0) {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
        }
        exchange.sendResponseHeaders(status, response.length);
        try (var outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }
}
