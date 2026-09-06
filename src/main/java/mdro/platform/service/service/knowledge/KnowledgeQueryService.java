package mdro.platform.service.service.knowledge;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import mdro.platform.service.dto.knowledge.request.KnowledgeQueryRequest;
import mdro.platform.service.dto.knowledge.response.KnowledgeQueryResponse;
import mdro.platform.service.dto.knowledge.response.KnowledgeQueryResult;
import mdro.platform.service.exception.knowledge.KnowledgeQueryException;
import mdro.platform.service.integration.ollama.embedding.EmbeddingService;
import mdro.platform.service.integration.qdrant.QdrantSearchResult;
import mdro.platform.service.integration.qdrant.QdrantService;
import mdro.platform.service.security.tenant.AuthorizedTenantContext;
import mdro.platform.service.security.tenant.TenantContextResolver;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KnowledgeQueryService {

    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;
    private final TenantContextResolver tenantContextResolver;
    private final KnowledgeChunkingProperties properties;

    public KnowledgeQueryService(
            EmbeddingService embeddingService,
            QdrantService qdrantService,
            TenantContextResolver tenantContextResolver,
            KnowledgeChunkingProperties properties) {
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
        this.tenantContextResolver = tenantContextResolver;
        this.properties = properties;
    }

    public KnowledgeQueryResponse query(KnowledgeQueryRequest request) {
        String question = request.question().trim();
        AuthorizedTenantContext tenantContext = tenantContextResolver.resolveCurrentTenant();
        UUID tenantId = tenantContext.tenantId();
        KnowledgeChunkingProperties.Query queryProperties = properties.getQuery();

        log.info(
                "Knowledge query started. Tenant ID: {}, Question length: {}",
                tenantId,
                question.length());

        try {
            List<Float> embedding = embeddingService.embed(question);
            log.info("Knowledge query embedding generated. Tenant ID: {}, Vector dimension: {}",
                    tenantId, embedding.size());

            List<QdrantSearchResult> searchResults = qdrantService.search(
                    embedding,
                    tenantId,
                    queryProperties.getDefaultLimit(),
                    queryProperties.getDefaultScoreThreshold());

            List<KnowledgeQueryResult> results = searchResults.stream()
                    .map(this::mapResult)
                    .toList();
            log.info("Qdrant query completed. Tenant ID: {}, Results found: {}",
                    tenantId, results.size());
            return new KnowledgeQueryResponse(question, results.size(), results);
        } catch (RuntimeException exception) {
            log.error("Knowledge query failed. Tenant ID: {}", tenantId, exception);
            throw new KnowledgeQueryException("Knowledge query could not be completed", exception);
        }
    }

    private KnowledgeQueryResult mapResult(QdrantSearchResult result) {
        return new KnowledgeQueryResult(
                result.pointId(),
                result.documentId(),
                result.chunkId(),
                result.content(),
                result.chunkIndex(),
                result.score());
    }
}
