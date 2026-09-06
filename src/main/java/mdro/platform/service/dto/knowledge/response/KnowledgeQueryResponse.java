package mdro.platform.service.dto.knowledge.response;

import java.util.List;

public record KnowledgeQueryResponse(
        String question,
        int totalResults,
        List<KnowledgeQueryResult> results) {
}
