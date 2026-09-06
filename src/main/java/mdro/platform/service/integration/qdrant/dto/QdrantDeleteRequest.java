package mdro.platform.service.integration.qdrant.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QdrantDeleteRequest {

    private QdrantFilter filter;
}
