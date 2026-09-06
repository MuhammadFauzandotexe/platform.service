package mdro.platform.service.integration.qdrant.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QdrantScrollResponse {

    private QdrantScrollResult result;
    private String status;
    private Double time;
}
