package mdro.platform.service.dto.knowledge.response;

import java.util.List;

public record KnowledgeListResponse(
        List<KnowledgeItemResponse> items,
        String nextCursor) {
}
