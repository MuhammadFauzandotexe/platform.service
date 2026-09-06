package mdro.platform.service.integration.qdrant.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QdrantQueryResult {

    private List<QdrantQueryPoint> points;
}
