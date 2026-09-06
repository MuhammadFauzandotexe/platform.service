package mdro.platform.service.integration.qdrant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QdrantScrollRequest {

    private Integer limit;

    @JsonProperty("with_payload")
    private Boolean withPayload;

    @JsonProperty("with_vector")
    private Boolean withVector;

    private String offset;
    private QdrantFilter filter;
}
