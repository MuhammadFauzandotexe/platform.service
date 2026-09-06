package mdro.platform.service.integration.qdrant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QdrantQueryRequest {

    private List<Float> query;
    private Integer limit;

    @JsonProperty("with_payload")
    private Boolean withPayload;

    @JsonProperty("score_threshold")
    private Double scoreThreshold;

    private QdrantFilter filter;
}
