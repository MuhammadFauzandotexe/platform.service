package mdro.platform.service.integration.ollama.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(OllamaProperties.class)
public class OllamaWebClientConfig {

    @Bean(name = "ollamaWebClient")
    public WebClient ollamaWebClient(
            WebClient.Builder builder,
            OllamaProperties properties) {
        Duration timeout = properties.getEmbedding().getTimeout();
        HttpClient httpClient = HttpClient.create().responseTimeout(timeout);

        return builder
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
