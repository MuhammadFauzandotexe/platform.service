package mdro.platform.service.dto.knowledge.response;

import java.util.List;
import java.util.UUID;

public record KnowledgeIngestionResponse(
        UUID tenantId,
        UUID documentId,
        int totalChunks,
        int successfulChunks,
        String status,
        List<KnowledgeChunkResult> chunks) {
}
