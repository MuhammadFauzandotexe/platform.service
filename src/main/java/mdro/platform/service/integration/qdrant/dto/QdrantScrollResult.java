package mdro.platform.service.integration.qdrant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QdrantScrollResult {

    private List<QdrantScrollPoint> points;

    @JsonProperty("next_page_offset")
    private String nextPageOffset;
}
