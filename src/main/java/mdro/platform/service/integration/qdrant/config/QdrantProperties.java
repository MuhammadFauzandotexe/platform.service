package mdro.platform.service.integration.qdrant.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "qdrant")
public class QdrantProperties {

    private String baseUrl;
    private String collectionName;
    private int vectorSize = 4096;
    private Duration timeout = Duration.ofSeconds(30);
}
