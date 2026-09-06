package mdro.platform.service.integration.ollama.embedding;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import mdro.platform.service.integration.ollama.config.OllamaProperties;
import mdro.platform.service.integration.ollama.dto.EmbeddingRequest;
import mdro.platform.service.integration.ollama.dto.EmbeddingResponse;
import mdro.platform.service.integration.ollama.exception.OllamaEmbeddingException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class OllamaEmbeddingService implements EmbeddingService {

    private final WebClient ollamaWebClient;
    private final OllamaProperties properties;

    public OllamaEmbeddingService(
            @Qualifier("ollamaWebClient") WebClient ollamaWebClient,
            OllamaProperties properties) {
        this.ollamaWebClient = ollamaWebClient;
        this.properties = properties;
    }

    @Override
    public List<Float> embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text to embed must not be null or blank");
        }

        EmbeddingResponse response;
        try {
            response = ollamaWebClient
                    .post()
                    .uri("/api/embed")
                    .bodyValue(new EmbeddingRequest(properties.getEmbedding().getModel(), text))
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            clientResponse -> clientResponse
                                    .bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(body -> Mono.error(new OllamaEmbeddingException(
                                            "Ollama embedding request failed with HTTP status "
                                                    + clientResponse.statusCode().value()
                                                    + (body.isBlank() ? "" : ": " + body),
                                            clientResponse.statusCode().value()))))
                    .bodyToMono(EmbeddingResponse.class)
                    .block(properties.getEmbedding().getTimeout());
        } catch (OllamaEmbeddingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new OllamaEmbeddingException(
                    "Could not call Ollama embedding API: " + exception.getMessage(),
                    exception);
        }

        List<Float> embedding = validateAndGetEmbedding(response);
        log.info(
                "Embedding generated successfully. Model: {}, Input length: {}, Vector dimension: {}",
                properties.getEmbedding().getModel(),
                text.length(),
                embedding.size());
        return embedding;
    }

    private List<Float> validateAndGetEmbedding(EmbeddingResponse response) {
        if (response == null
                || response.getEmbeddings() == null
                || response.getEmbeddings().isEmpty()
                || response.getEmbeddings().get(0) == null
                || response.getEmbeddings().get(0).isEmpty()) {
            throw new OllamaEmbeddingException("Ollama returned empty embeddings");
        }
        return response.getEmbeddings().get(0);
    }
}
