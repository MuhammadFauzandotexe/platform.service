package mdro.platform.service.integration.qdrant.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QdrantQueryPoint {

    private String id;
    private Integer version;
    private Double score;
    private QdrantPayload payload;
}
