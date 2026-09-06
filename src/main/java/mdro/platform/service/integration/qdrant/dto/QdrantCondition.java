package mdro.platform.service.integration.qdrant.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QdrantCondition {

    private String key;
    private QdrantMatch match;
}
