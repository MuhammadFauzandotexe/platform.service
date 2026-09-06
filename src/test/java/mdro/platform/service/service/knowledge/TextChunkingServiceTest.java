package mdro.platform.service.service.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextChunkingServiceTest {

    @Test
    void prefersNaturalSentenceBoundaries() {
        KnowledgeChunkingProperties properties = new KnowledgeChunkingProperties();
        properties.setChunkSize(100);
        properties.setChunkOverlap(10);
        TextChunkingService service = new TextChunkingService(properties);

        List<String> chunks = service.chunk(
                "Produk dapat dikembalikan maksimal 7 hari setelah pembelian. "
                        + "Produk harus dalam kondisi baik dan belum digunakan.");

        assertEquals(2, chunks.size());
        assertEquals(
                "Produk dapat dikembalikan maksimal 7 hari setelah pembelian.",
                chunks.get(0));
        assertEquals(
                "Produk harus dalam kondisi baik dan belum digunakan.",
                chunks.get(1));
    }

    @Test
    void appliesConfiguredChunkSizeAndOverlap() {
        KnowledgeChunkingProperties properties = new KnowledgeChunkingProperties();
        properties.setChunkSize(10);
        properties.setChunkOverlap(2);
        TextChunkingService service = new TextChunkingService(properties);

        List<String> chunks = service.chunk("1234567890abcdefghij");

        assertEquals(List.of("1234567890", "90abcdefgh", "ghij"), chunks);
    }
}
