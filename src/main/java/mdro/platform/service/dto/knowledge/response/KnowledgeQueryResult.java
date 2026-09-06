package mdro.platform.service.dto.knowledge.response;

import java.util.UUID;

public record KnowledgeQueryResult(
        UUID pointId,
        UUID documentId,
        UUID chunkId,
        String content,
        Integer chunkIndex,
        double score) {
}
