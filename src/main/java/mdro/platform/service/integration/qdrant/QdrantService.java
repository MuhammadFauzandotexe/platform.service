package mdro.platform.service.integration.qdrant;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface QdrantService {

    void upsertVector(UUID pointId, List<Float> vector, Map<String, Object> payload);

    List<QdrantSearchResult> search(
            List<Float> queryVector,
            UUID tenantId,
            Integer limit,
            Double scoreThreshold);
}
