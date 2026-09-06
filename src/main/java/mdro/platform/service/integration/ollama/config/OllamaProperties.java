package mdro.platform.service.integration.ollama.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ollama")
public class OllamaProperties {

    private String baseUrl;
    private Embedding embedding = new Embedding();

    @Getter
    @Setter
    public static class Embedding {

        private String model;
        private Duration timeout = Duration.ofSeconds(30);
    }
}
