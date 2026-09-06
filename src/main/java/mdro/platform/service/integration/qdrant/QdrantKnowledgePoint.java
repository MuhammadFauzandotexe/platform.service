package mdro.platform.service.integration.qdrant;

import java.time.Instant;
import java.util.UUID;

public record QdrantKnowledgePoint(
        UUID pointId,
        UUID documentId,
        UUID chunkId,
        String content,
        Integer chunkIndex,
        Instant createdAt,
        UUID tenantId) {
}
