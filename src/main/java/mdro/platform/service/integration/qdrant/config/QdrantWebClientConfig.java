package mdro.platform.service.integration.qdrant.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

@Configuration
@EnableConfigurationProperties(QdrantProperties.class)
public class QdrantWebClientConfig {

    @Bean(name = "qdrantWebClient")
    public WebClient qdrantWebClient(
            WebClient.Builder builder,
            QdrantProperties properties) {
        Duration timeout = properties.getTimeout();
        HttpClient httpClient = HttpClient.create().responseTimeout(timeout);

        return builder
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
