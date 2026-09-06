package mdro.platform.service.integration.qdrant.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QdrantScrollPoint {

    private String id;
    private QdrantPayload payload;
}
