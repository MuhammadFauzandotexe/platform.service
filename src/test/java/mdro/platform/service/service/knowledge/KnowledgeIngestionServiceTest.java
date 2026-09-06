package mdro.platform.service.service.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import mdro.platform.service.dto.knowledge.request.KnowledgeIngestionRequest;
import mdro.platform.service.exception.knowledge.KnowledgeIngestionException;
import mdro.platform.service.integration.ollama.embedding.EmbeddingService;
import mdro.platform.service.integration.qdrant.QdrantService;
import mdro.platform.service.integration.qdrant.config.QdrantProperties;
import mdro.platform.service.security.tenant.AuthorizedTenantContext;
import mdro.platform.service.entity.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KnowledgeIngestionServiceTest {

    private final TextChunkingService chunkingService = mock(TextChunkingService.class);
    private final EmbeddingService embeddingService = mock(EmbeddingService.class);
    private final QdrantService qdrantService = mock(QdrantService.class);
    private final QdrantProperties qdrantProperties = new QdrantProperties();
    private KnowledgeIngestionService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();
    private final AuthorizedTenantContext tenantContext = new AuthorizedTenantContext(
            tenantId,
            tenantId,
            mock(Account.class));

    @BeforeEach
    void setUp() {
        qdrantProperties.setVectorSize(4);
        service = new KnowledgeIngestionService(
                chunkingService,
                embeddingService,
                qdrantService,
                qdrantProperties);
    }

    @Test
    void embedsAndUpsertsEveryChunkWithMetadata() {
        when(chunkingService.chunk("document content")).thenReturn(List.of("first", "second"));
        when(embeddingService.embed("first")).thenReturn(vector());
        when(embeddingService.embed("second")).thenReturn(vector());

        var response = service.ingest(tenantContext, new KnowledgeIngestionRequest(
                documentId,
                "document content"));

        assertEquals(2, response.totalChunks());
        assertEquals(2, response.successfulChunks());
        assertEquals("SUCCESS", response.status());
        assertEquals(2, response.chunks().size());
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(qdrantService, org.mockito.Mockito.times(2))
                .upsertVector(any(), eq(vector()), payloadCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(
                tenantId.toString(),
                payloadCaptor.getAllValues().get(0).get("tenantId"));
        org.junit.jupiter.api.Assertions.assertFalse(
                payloadCaptor.getAllValues().get(0).containsKey("tenatId"));
    }

    @Test
    void stopsBeforeQdrantWhenEmbeddingDimensionIsInvalid() {
        when(chunkingService.chunk("document content")).thenReturn(List.of("first"));
        when(embeddingService.embed("first")).thenReturn(List.of(1.0f, 2.0f));

        KnowledgeIngestionException exception = assertThrows(
                KnowledgeIngestionException.class,
                () -> service.ingest(tenantContext, new KnowledgeIngestionRequest(
                        documentId,
                        "document content")));

        assertEquals(
                "Knowledge ingestion failed at chunk index 0. Successfully stored chunks before failure: 0",
                exception.getMessage());
        verify(qdrantService, never()).upsertVector(any(), any(), any());
    }

    private List<Float> vector() {
        return List.of(1.0f, 2.0f, 3.0f, 4.0f);
    }
}
