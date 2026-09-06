package mdro.platform.service.dto.knowledge.response;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeItemResponse(
        UUID pointId,
        UUID documentId,
        UUID chunkId,
        String content,
        Integer chunkIndex,
        Instant createdAt) {
}
