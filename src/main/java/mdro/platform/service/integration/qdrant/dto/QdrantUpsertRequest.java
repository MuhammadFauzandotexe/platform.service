package mdro.platform.service.integration.qdrant.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QdrantUpsertRequest {

    private List<QdrantPoint> points;
}
