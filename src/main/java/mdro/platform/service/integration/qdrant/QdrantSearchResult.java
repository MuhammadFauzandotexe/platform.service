package mdro.platform.service.integration.qdrant;

import java.util.UUID;

public record QdrantSearchResult(
        UUID pointId,
        double score,
        UUID documentId,
        UUID chunkId,
        String content,
        Integer chunkIndex) {
}
