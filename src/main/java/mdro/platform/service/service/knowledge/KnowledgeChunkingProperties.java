package mdro.platform.service.service.knowledge;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "knowledge")
public class KnowledgeChunkingProperties {

    private int chunkSize = 1000;
    private int chunkOverlap = 150;
}
