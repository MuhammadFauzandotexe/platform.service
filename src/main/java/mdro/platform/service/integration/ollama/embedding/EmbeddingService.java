package mdro.platform.service.integration.ollama.embedding;

import java.util.List;

public interface EmbeddingService {

    /**
     * Generates one embedding vector for the supplied text.
     *
     * Example:
     * {@code embeddingService.embed("Produk dapat dikembalikan maksimal 7 hari setelah pembelian.")}
     */
    List<Float> embed(String text);
}
