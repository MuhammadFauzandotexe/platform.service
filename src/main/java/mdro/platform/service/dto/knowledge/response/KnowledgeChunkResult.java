package mdro.platform.service.dto.knowledge.response;

import java.util.UUID;

public record KnowledgeChunkResult(
        UUID pointId,
        UUID chunkId,
        int chunkIndex,
        boolean success) {
}
