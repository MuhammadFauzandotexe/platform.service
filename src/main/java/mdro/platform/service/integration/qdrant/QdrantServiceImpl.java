package mdro.platform.service.integration.qdrant;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import mdro.platform.service.integration.qdrant.config.QdrantProperties;
import mdro.platform.service.integration.qdrant.dto.QdrantCondition;
import mdro.platform.service.integration.qdrant.dto.QdrantFilter;
import mdro.platform.service.integration.qdrant.dto.QdrantMatch;
import mdro.platform.service.integration.qdrant.dto.QdrantPoint;
import mdro.platform.service.integration.qdrant.dto.QdrantPayload;
import mdro.platform.service.integration.qdrant.dto.QdrantQueryPoint;
import mdro.platform.service.integration.qdrant.dto.QdrantQueryRequest;
import mdro.platform.service.integration.qdrant.dto.QdrantQueryResponse;
import mdro.platform.service.integration.qdrant.dto.QdrantUpsertRequest;
import mdro.platform.service.integration.qdrant.exception.QdrantException;
import mdro.platform.service.integration.qdrant.exception.QdrantIntegrationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
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

    @Override
    public List<QdrantSearchResult> search(
            List<Float> queryVector,
            UUID tenantId,
            Integer limit,
            Double scoreThreshold) {
        int resolvedLimit = resolveLimit(limit);
        double resolvedScoreThreshold = resolveScoreThreshold(scoreThreshold);
        validateSearchInput(queryVector, tenantId);

        log.info(
                "Qdrant knowledge search started. Tenant ID: {}, Vector dimension: {}, Limit: {}, Score threshold: {}",
                tenantId,
                queryVector.size(),
                resolvedLimit,
                resolvedScoreThreshold);

        QdrantQueryRequest request = new QdrantQueryRequest(
                queryVector,
                resolvedLimit,
                true,
                resolvedScoreThreshold,
                new QdrantFilter(List.of(
                        new QdrantCondition(TENANT_ID_FIELD, new QdrantMatch(tenantId.toString())))));

        QdrantQueryResponse response;
        try {
            response = qdrantWebClient
                    .post()
                    .uri("/collections/{collectionName}/points/query", properties.getCollectionName())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            clientResponse -> Mono.error(new QdrantIntegrationException(
                                    "Qdrant knowledge search failed with HTTP status "
                                            + clientResponse.statusCode().value(),
                                    clientResponse.statusCode().value())))
                    .bodyToMono(QdrantQueryResponse.class)
                    .block(properties.getTimeout());
        } catch (QdrantIntegrationException exception) {
            log.error("Qdrant knowledge search failed. Tenant ID: {}, Status: {}",
                    tenantId, exception.getHttpStatusCode(), exception);
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Qdrant knowledge search could not be completed. Tenant ID: {}",
                    tenantId, exception);
            throw new QdrantIntegrationException(
                    "Could not complete Qdrant knowledge search", exception);
        }

        List<QdrantSearchResult> results = mapSearchResponse(response);
        log.info(
                "Qdrant knowledge search successful. Tenant ID: {}, Results found: {}",
                tenantId,
                results.size());
        return results;
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

    private void validateSearchInput(List<Float> queryVector, UUID tenantId) {
        if (queryVector == null || queryVector.isEmpty()) {
            throw new IllegalArgumentException("Query vector must not be null or empty");
        }
        if (queryVector.size() != properties.getVectorSize()) {
            throw new IllegalArgumentException(
                    "Expected vector dimension: "
                            + properties.getVectorSize()
                            + ", Actual vector dimension: "
                            + queryVector.size());
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required for Qdrant search");
        }
    }

    private int resolveLimit(Integer limit) {
        int resolvedLimit = limit == null ? properties.getDefaultLimit() : limit;
        if (resolvedLimit <= 0 || resolvedLimit > properties.getMaxLimit()) {
            throw new IllegalArgumentException(
                    "Search limit must be between 1 and " + properties.getMaxLimit());
        }
        return resolvedLimit;
    }

    private double resolveScoreThreshold(Double scoreThreshold) {
        double resolvedScoreThreshold = scoreThreshold == null
                ? properties.getDefaultScoreThreshold()
                : scoreThreshold;
        if (!Double.isFinite(resolvedScoreThreshold)
                || resolvedScoreThreshold < 0
                || resolvedScoreThreshold > 1) {
            throw new IllegalArgumentException("scoreThreshold must be between 0 and 1");
        }
        return resolvedScoreThreshold;
    }

    private List<QdrantSearchResult> mapSearchResponse(QdrantQueryResponse response) {
        if (response == null
                || !"ok".equals(response.getStatus())
                || response.getResult() == null
                || response.getResult().getPoints() == null) {
            throw new QdrantIntegrationException("Qdrant returned an invalid search response");
        }

        return response.getResult().getPoints().stream()
                .map(this::mapSearchPoint)
                .toList();
    }

    private QdrantSearchResult mapSearchPoint(QdrantQueryPoint point) {
        if (point == null || point.getId() == null || point.getScore() == null
                || point.getPayload() == null
                || point.getPayload().getDocumentId() == null
                || point.getPayload().getChunkId() == null) {
            throw new QdrantIntegrationException("Qdrant returned an invalid search result point");
        }

        QdrantPayload payload = point.getPayload();
        try {
            return new QdrantSearchResult(
                    UUID.fromString(point.getId()),
                    point.getScore(),
                    UUID.fromString(payload.getDocumentId()),
                    UUID.fromString(payload.getChunkId()),
                    payload.getContent(),
                    payload.getChunkIndex());
        } catch (IllegalArgumentException exception) {
            throw new QdrantIntegrationException(
                    "Qdrant returned a search result with invalid identifiers", exception);
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
