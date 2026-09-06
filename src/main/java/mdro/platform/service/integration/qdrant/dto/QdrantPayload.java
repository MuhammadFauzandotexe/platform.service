package mdro.platform.service.integration.qdrant.dto;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QdrantPayload {

    private String tenantId;
    private String documentId;
    private String chunkId;
    private String content;
    private Integer chunkIndex;
    private Instant createdAt;
}
