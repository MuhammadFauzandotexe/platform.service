package mdro.platform.service.service.knowledge;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import mdro.platform.service.dto.knowledge.response.KnowledgeItemResponse;
import mdro.platform.service.dto.knowledge.response.KnowledgeListResponse;
import mdro.platform.service.exception.knowledge.KnowledgeManagementException;
import mdro.platform.service.exception.knowledge.KnowledgeNotFoundException;
import mdro.platform.service.integration.qdrant.QdrantKnowledgePage;
import mdro.platform.service.integration.qdrant.QdrantKnowledgePoint;
import mdro.platform.service.integration.qdrant.QdrantService;
import mdro.platform.service.security.tenant.AuthorizedTenantContext;
import mdro.platform.service.security.tenant.TenantContextResolver;
import mdro.platform.service.integration.qdrant.config.QdrantProperties;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KnowledgeManagementService {

    private final QdrantService qdrantService;
    private final TenantContextResolver tenantContextResolver;
    private final QdrantProperties qdrantProperties;

    public KnowledgeManagementService(
            QdrantService qdrantService,
            TenantContextResolver tenantContextResolver,
            QdrantProperties qdrantProperties) {
        this.qdrantService = qdrantService;
        this.tenantContextResolver = tenantContextResolver;
        this.qdrantProperties = qdrantProperties;
    }

    public KnowledgeListResponse list(Integer limit, String cursor) {
        AuthorizedTenantContext tenantContext = tenantContextResolver.resolveCurrentTenant();
        UUID tenantId = tenantContext.tenantId();
        int resolvedLimit = limit == null
                ? qdrantProperties.getKnowledgeManagementDefaultLimit()
                : limit;
        validateLimit(resolvedLimit);
        validateCursor(cursor);

        log.info("Knowledge list requested. Tenant ID: {}, Limit: {}", tenantId, resolvedLimit);
        try {
            QdrantKnowledgePage page = qdrantService.scrollKnowledge(
                    tenantId, resolvedLimit, cursor);
            List<KnowledgeItemResponse> items = page.points().stream()
                    .map(this::mapItem)
                    .toList();
            log.info("Knowledge list completed. Tenant ID: {}, Results: {}",
                    tenantId, items.size());
            return new KnowledgeListResponse(items, page.nextCursor());
        } catch (KnowledgeManagementException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Knowledge list failed. Tenant ID: {}", tenantId, exception);
            throw new KnowledgeManagementException(
                    "Knowledge list could not be completed", exception);
        }
    }

    public void removeKnowledge(UUID pointId) {
        AuthorizedTenantContext tenantContext = tenantContextResolver.resolveCurrentTenant();
        UUID tenantId = tenantContext.tenantId();
        log.info("Knowledge delete requested. Tenant ID: {}, Point ID: {}", tenantId, pointId);
        try {
            if (!qdrantService.deleteKnowledgeForTenant(pointId, tenantId)) {
                throw new KnowledgeNotFoundException();
            }
            log.info("Knowledge deleted successfully. Tenant ID: {}, Point ID: {}",
                    tenantId, pointId);
        } catch (KnowledgeNotFoundException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Knowledge delete failed. Tenant ID: {}, Point ID: {}",
                    tenantId, pointId, exception);
            throw new KnowledgeManagementException(
                    "Knowledge delete could not be completed", exception);
        }
    }

    private void validateLimit(int limit) {
        if (limit <= 0 || limit > qdrantProperties.getKnowledgeManagementMaxLimit()) {
            throw new IllegalArgumentException(
                    "Knowledge limit must be between 1 and "
                            + qdrantProperties.getKnowledgeManagementMaxLimit());
        }
    }

    private void validateCursor(String cursor) {
        if (cursor != null && !cursor.isBlank()) {
            try {
                UUID.fromString(cursor);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Cursor is invalid", exception);
            }
        }
    }

    private KnowledgeItemResponse mapItem(QdrantKnowledgePoint point) {
        return new KnowledgeItemResponse(
                point.pointId(),
                point.documentId(),
                point.chunkId(),
                point.content(),
                point.chunkIndex(),
                point.createdAt());
    }
}
