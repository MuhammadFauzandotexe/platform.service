package mdro.platform.service.integration.qdrant;

import java.util.List;

public record QdrantKnowledgePage(
        List<QdrantKnowledgePoint> points,
        String nextCursor) {
}
