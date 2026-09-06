package mdro.platform.service.service.knowledge;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import mdro.platform.service.dto.knowledge.request.KnowledgeIngestionRequest;
import mdro.platform.service.dto.knowledge.response.KnowledgeChunkResult;
import mdro.platform.service.dto.knowledge.response.KnowledgeIngestionResponse;
import mdro.platform.service.exception.knowledge.KnowledgeIngestionException;
import mdro.platform.service.integration.ollama.embedding.EmbeddingService;
import mdro.platform.service.integration.qdrant.QdrantService;
import mdro.platform.service.integration.qdrant.config.QdrantProperties;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KnowledgeIngestionService {

    private final TextChunkingService textChunkingService;
    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;
    private final QdrantProperties qdrantProperties;

    public KnowledgeIngestionService(
            TextChunkingService textChunkingService,
            EmbeddingService embeddingService,
            QdrantService qdrantService,
            QdrantProperties qdrantProperties) {
        this.textChunkingService = textChunkingService;
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
        this.qdrantProperties = qdrantProperties;
    }

    public KnowledgeIngestionResponse ingest(KnowledgeIngestionRequest request) {
        List<String> chunks = textChunkingService.chunk(request.content());
        List<KnowledgeChunkResult> results = new ArrayList<>();

        for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
            UUID chunkId = UUID.randomUUID();
            UUID pointId = UUID.randomUUID();
            String chunk = chunks.get(chunkIndex);

            try {
                List<Float> embedding = embeddingService.embed(chunk);
                validateEmbedding(embedding);

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("tenantId", request.tenantId().toString());
                payload.put("documentId", request.documentId().toString());
                payload.put("chunkId", chunkId.toString());
                payload.put("content", chunk);
                payload.put("chunkIndex", chunkIndex);
                payload.put("createdAt", Instant.now().toString());

                qdrantService.upsertVector(pointId, embedding, payload);
                results.add(new KnowledgeChunkResult(pointId, chunkId, chunkIndex, true));
                log.info(
                        "Knowledge chunk ingested successfully. Tenant ID: {}, Document ID: {}, Chunk index: {}, Embedding dimension: {}, Point ID: {}",
                        request.tenantId(),
                        request.documentId(),
                        chunkIndex,
                        embedding.size(),
                        pointId);
            } catch (RuntimeException exception) {
                log.error(
                        "Knowledge chunk ingestion failed. Tenant ID: {}, Document ID: {}, Chunk index: {}, Point ID: {}",
                        request.tenantId(),
                        request.documentId(),
                        chunkIndex,
                        pointId,
                        exception);
                throw new KnowledgeIngestionException(
                        "Knowledge ingestion failed at chunk index " + chunkIndex
                                + ". Successfully stored chunks before failure: " + results.size(),
                        exception);
            }
        }

        return new KnowledgeIngestionResponse(
                request.tenantId(),
                request.documentId(),
                chunks.size(),
                results.size(),
                "SUCCESS",
                List.copyOf(results));
    }

    private void validateEmbedding(List<Float> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            throw new IllegalArgumentException("Embedding must not be null or empty");
        }
        if (embedding.size() != qdrantProperties.getVectorSize()) {
            throw new IllegalArgumentException(
                    "INVALID_EMBEDDING_DIMENSION. Expected: "
                            + qdrantProperties.getVectorSize()
                            + ", Actual: "
                            + embedding.size());
        }
    }
}
