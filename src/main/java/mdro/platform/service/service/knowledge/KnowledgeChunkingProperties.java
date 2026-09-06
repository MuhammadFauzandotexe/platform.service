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
    private Query query = new Query();

    @Getter
    @Setter
    public static class Query {

        private int defaultLimit = 5;
        private int maxLimit = 10;
        private double defaultScoreThreshold = 0.0;
    }
}
