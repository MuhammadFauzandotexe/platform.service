package mdro.platform.service.integration.qdrant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QdrantCondition {

    private String key;
    private QdrantMatch match;

    @JsonProperty("has_id")
    private List<String> hasId;

    public QdrantCondition(String key, QdrantMatch match) {
        this(key, match, null);
    }

    public QdrantCondition(List<String> hasId) {
        this(null, null, hasId);
    }
}
