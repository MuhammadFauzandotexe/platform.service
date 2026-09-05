package mdro.platform.service.integration.whatsapp.client;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import mdro.platform.service.integration.whatsapp.config.WhatsAppGatewayProperties;
import mdro.platform.service.integration.whatsapp.dto.CreateWhatsAppSessionRequest;
import mdro.platform.service.integration.whatsapp.dto.WhatsAppGatewayErrorResponse;
import mdro.platform.service.integration.whatsapp.dto.WhatsAppSessionResponse;
import mdro.platform.service.integration.whatsapp.exception.WhatsAppGatewayException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class WhatsAppGatewayClient {

    private static final ParameterizedTypeReference<List<WhatsAppSessionResponse>> SESSION_LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient webClient;

    public WhatsAppGatewayClient(WebClient.Builder webClientBuilder, WhatsAppGatewayProperties properties) {
        this.webClient = webClientBuilder.baseUrl(properties.getBaseUrl()).build();
    }

    public WhatsAppSessionResponse createSession(UUID tenantId, UUID sessionId) {
        log.debug("Calling WhatsApp Gateway to create session for tenant {}", tenantId);
        return execute(
                "create session",
                webClient
                        .post()
                        .uri("/sessions")
                        .bodyValue(new CreateWhatsAppSessionRequest(tenantId, sessionId))
                        .exchangeToMono(response -> readResponse(response, WhatsAppSessionResponse.class)));
    }

    public List<WhatsAppSessionResponse> getSessionsByTenant(UUID tenantId) {
        log.debug("Calling WhatsApp Gateway to get sessions for tenant {}", tenantId);
        return execute(
                "get sessions",
                webClient
                        .get()
                        .uri("/sessions/{tenantId}", tenantId)
                        .exchangeToMono(this::readResponse));
    }

    public WhatsAppSessionResponse disconnectSession(UUID tenantId, UUID sessionId) {
        log.debug("Calling WhatsApp Gateway to disconnect session for tenant {}", tenantId);
        return execute(
                "disconnect session",
                webClient
                        .post()
                        .uri("/sessions/{tenantId}/{sessionId}/disconnect", tenantId, sessionId)
                        .exchangeToMono(response -> readResponse(response, WhatsAppSessionResponse.class)));
    }

    public void deleteSession(UUID tenantId, UUID sessionId) {
        log.debug("Calling WhatsApp Gateway to delete session for tenant {}", tenantId);
        execute(
                "delete session",
                webClient
                        .delete()
                        .uri("/sessions/{tenantId}/{sessionId}", tenantId, sessionId)
                        .exchangeToMono(this::readNoContentResponse));
    }

    private <T> Mono<T> readResponse(
            org.springframework.web.reactive.function.client.ClientResponse response,
            Class<T> responseType) {
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(responseType);
        }
        return readError(response);
    }

    private <T> Mono<T> readResponse(
            ClientResponse response) {
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono((ParameterizedTypeReference<T>) WhatsAppGatewayClient.SESSION_LIST_TYPE);
        }
        return readError(response);
    }

    private Mono<Void> readNoContentResponse(
            org.springframework.web.reactive.function.client.ClientResponse response) {
        if (response.statusCode().is2xxSuccessful()) {
            return response.releaseBody();
        }
        return readError(response);
    }

    private <T> Mono<T> readError(
            org.springframework.web.reactive.function.client.ClientResponse response) {
        HttpStatusCode statusCode = response.statusCode();
        return response.bodyToMono(WhatsAppGatewayErrorResponse.class)
                .defaultIfEmpty(new WhatsAppGatewayErrorResponse(null))
                .flatMap(error -> Mono.error(new WhatsAppGatewayException(
                        statusCode.value(),
                        error.error())));
    }

    private <T> T execute(String operation, Mono<T> request) {
        try {
            return request
                    .doOnError(error -> log.warn("WhatsApp Gateway {} failed", operation))
                    .onErrorMap(
                            error -> !(error instanceof WhatsAppGatewayException),
                            error -> new WhatsAppGatewayException(null, error.getMessage(), error))
                    .block();
        } catch (WhatsAppGatewayException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new WhatsAppGatewayException(null, exception.getMessage(), exception);
        }
    }
}
