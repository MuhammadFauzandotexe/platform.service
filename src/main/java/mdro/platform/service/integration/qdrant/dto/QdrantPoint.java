package mdro.platform.service.integration.qdrant.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QdrantPoint {

    private UUID id;
    private List<Float> vector;
    private Map<String, Object> payload;
}
