package mdro.platform.service.integration.qdrant;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import mdro.platform.service.integration.qdrant.config.QdrantProperties;
import mdro.platform.service.integration.qdrant.dto.QdrantPoint;
import mdro.platform.service.integration.qdrant.dto.QdrantUpsertRequest;
import mdro.platform.service.integration.qdrant.exception.QdrantException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

@Slf4j
@Service
public class QdrantServiceImpl implements QdrantService {

    private static final String TENANT_ID_FIELD = "tenantId";

    private final WebClient qdrantWebClient;
    private final QdrantProperties properties;

    public QdrantServiceImpl(
            @Qualifier("qdrantWebClient") WebClient qdrantWebClient,
            QdrantProperties properties) {
        this.qdrantWebClient = qdrantWebClient;
        this.properties = properties;
    }

    @Override
    public void upsertVector(UUID pointId, List<Float> vector, Map<String, Object> payload) {
        validate(pointId, vector, payload);

        QdrantUpsertRequest request = new QdrantUpsertRequest(
                List.of(new QdrantPoint(pointId, vector, payload)));

        try {
            JsonNode response = qdrantWebClient
                    .put()
                    .uri("/collections/{collectionName}/points", properties.getCollectionName())
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            clientResponse -> clientResponse
                                    .bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(body -> Mono.error(new QdrantException(
                                            "Failed to upsert vector to Qdrant. HTTP status: "
                                                    + clientResponse.statusCode().value()
                                                    + (body.isBlank() ? "" : ", response: " + body),
                                            clientResponse.statusCode().value()))))
                    .bodyToMono(JsonNode.class)
                    .block(properties.getTimeout());

            validateResponse(response);
        } catch (QdrantException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new QdrantException(
                    "Failed to upsert vector to Qdrant: " + exception.getMessage(),
                    exception);
        }

        log.info(
                "Qdrant vector upsert successful. Collection: {}, Point ID: {}, Vector dimension: {}, Tenant ID: {}",
                properties.getCollectionName(),
                pointId,
                vector.size(),
                payload.get(TENANT_ID_FIELD));
    }

    private void validate(UUID pointId, List<Float> vector, Map<String, Object> payload) {
        if (pointId == null) {
            throw new IllegalArgumentException("pointId is required");
        }
        if (vector == null || vector.isEmpty()) {
            throw new IllegalArgumentException("Vector must not be null or empty");
        }
        if (vector.size() != properties.getVectorSize()) {
            throw new IllegalArgumentException(
                    "Expected vector dimension: "
                            + properties.getVectorSize()
                            + ", Actual vector dimension: "
                            + vector.size());
        }
        if (payload == null) {
            throw new IllegalArgumentException("Qdrant payload must not be null");
        }
        if (!payload.containsKey(TENANT_ID_FIELD)
                || payload.get(TENANT_ID_FIELD) == null) {
            throw new IllegalArgumentException("tenantId is required in Qdrant payload");
        }
    }

    private void validateResponse(JsonNode response) {
        if (response == null
                || !response.isObject()
                || !response.has("status")
                || !"ok".equals(response.path("status").asText())) {
            throw new QdrantException("Qdrant returned an invalid response");
        }
    }
}
