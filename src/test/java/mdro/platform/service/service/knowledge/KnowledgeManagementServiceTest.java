package mdro.platform.service.service.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import mdro.platform.service.exception.knowledge.KnowledgeNotFoundException;
import mdro.platform.service.integration.qdrant.QdrantKnowledgePage;
import mdro.platform.service.integration.qdrant.QdrantKnowledgePoint;
import mdro.platform.service.integration.qdrant.QdrantService;
import mdro.platform.service.integration.qdrant.config.QdrantProperties;
import mdro.platform.service.security.tenant.AuthorizedTenantContext;
import mdro.platform.service.security.tenant.TenantContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KnowledgeManagementServiceTest {

    private final QdrantService qdrantService = mock(QdrantService.class);
    private final TenantContextResolver tenantContextResolver = mock(TenantContextResolver.class);
    private final QdrantProperties qdrantProperties = new QdrantProperties();
    private final UUID tenantId = UUID.randomUUID();
    private final UUID pointId = UUID.randomUUID();
    private KnowledgeManagementService service;

    @BeforeEach
    void setUp() {
        when(tenantContextResolver.resolveCurrentTenant())
                .thenReturn(new AuthorizedTenantContext(null, tenantId, null));
        service = new KnowledgeManagementService(
                qdrantService, tenantContextResolver, qdrantProperties);
    }

    @Test
    void listsOnlyUsingTenantResolvedFromContextAndMapsPage() {
        QdrantKnowledgePoint point = new QdrantKnowledgePoint(
                pointId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "content",
                0,
                Instant.parse("2026-09-06T08:01:04.299649600Z"),
                tenantId);
        when(qdrantService.scrollKnowledge(tenantId, 20, "0f1e2d3c-4b5a-4678-9012-345678901234"))
                .thenReturn(new QdrantKnowledgePage(List.of(point), "next"));

        var response = service.list(20, "0f1e2d3c-4b5a-4678-9012-345678901234");

        assertEquals(1, response.items().size());
        assertEquals("next", response.nextCursor());
        verify(qdrantService).scrollKnowledge(
                tenantId, 20, "0f1e2d3c-4b5a-4678-9012-345678901234");
    }

    @Test
    void rejectsInvalidCursorBeforeCallingQdrant() {
        assertThrows(IllegalArgumentException.class, () -> service.list(20, "not-a-cursor"));
        verifyNoInteractions(qdrantService);
    }

    @Test
    void reportsUnknownOrForeignPointAsNotFoundAndDoesNotDelete() {
        when(qdrantService.deleteKnowledgeForTenant(pointId, tenantId)).thenReturn(false);

        assertThrows(KnowledgeNotFoundException.class, () -> service.removeKnowledge(pointId));

        verify(qdrantService).deleteKnowledgeForTenant(pointId, tenantId);
    }
}
