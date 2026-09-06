package mdro.platform.service.service.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import mdro.platform.service.dto.knowledge.request.KnowledgeQueryRequest;
import mdro.platform.service.dto.knowledge.response.KnowledgeQueryResponse;
import mdro.platform.service.integration.ollama.embedding.EmbeddingService;
import mdro.platform.service.integration.qdrant.QdrantSearchResult;
import mdro.platform.service.integration.qdrant.QdrantService;
import mdro.platform.service.security.tenant.AuthorizedTenantContext;
import mdro.platform.service.security.tenant.TenantContextResolver;
import mdro.platform.service.entity.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KnowledgeQueryServiceTest {

    private final EmbeddingService embeddingService = mock(EmbeddingService.class);
    private final QdrantService qdrantService = mock(QdrantService.class);
    private final TenantContextResolver tenantContextResolver = mock(TenantContextResolver.class);
    private final KnowledgeChunkingProperties properties = new KnowledgeChunkingProperties();
    private final UUID tenantId = UUID.randomUUID();
    private KnowledgeQueryService service;

    @BeforeEach
    void setUp() {
        properties.getQuery().setDefaultLimit(5);
        properties.getQuery().setDefaultScoreThreshold(0.0);
        service = new KnowledgeQueryService(
                embeddingService,
                qdrantService,
                tenantContextResolver,
                properties);
    }

    @Test
    void resolvesTenantServerSideEmbedsTrimmedQuestionAndMapsResults() {
        List<Float> embedding = List.of(1.0f, 2.0f);
        QdrantSearchResult searchResult = new QdrantSearchResult(
                UUID.randomUUID(),
                0.69882184,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Produk dapat dikembalikan maksimal 7 hari.",
                0);
        when(tenantContextResolver.resolveCurrentTenant())
                .thenReturn(new AuthorizedTenantContext(tenantId, tenantId, mock(Account.class)));
        when(embeddingService.embed("Berapa lama produk bisa dikembalikan?"))
                .thenReturn(embedding);
        when(qdrantService.search(embedding, tenantId, 5, 0.0))
                .thenReturn(List.of(searchResult));

        KnowledgeQueryResponse response = service.query(
                new KnowledgeQueryRequest("  Berapa lama produk bisa dikembalikan?  "));

        assertEquals("Berapa lama produk bisa dikembalikan?", response.question());
        assertEquals(1, response.totalResults());
        assertEquals(searchResult.pointId(), response.results().get(0).pointId());
        assertEquals(searchResult.score(), response.results().get(0).score());
        verify(tenantContextResolver).resolveCurrentTenant();
        verify(embeddingService).embed("Berapa lama produk bisa dikembalikan?");
        verify(qdrantService).search(eq(embedding), eq(tenantId), eq(5), eq(0.0));
    }

    @Test
    void returnsEmptyResultWithoutError() {
        List<Float> embedding = List.of(1.0f, 2.0f);
        when(tenantContextResolver.resolveCurrentTenant())
                .thenReturn(new AuthorizedTenantContext(tenantId, tenantId, mock(Account.class)));
        when(embeddingService.embed("abc")).thenReturn(embedding);
        when(qdrantService.search(embedding, tenantId, 5, 0.0))
                .thenReturn(List.of());

        KnowledgeQueryResponse response = service.query(new KnowledgeQueryRequest("abc"));

        assertEquals("abc", response.question());
        assertEquals(0, response.totalResults());
        assertEquals(List.of(), response.results());
    }
}
