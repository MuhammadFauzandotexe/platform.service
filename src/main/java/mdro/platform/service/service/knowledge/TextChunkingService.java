package mdro.platform.service.service.knowledge;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TextChunkingService {

    private final KnowledgeChunkingProperties properties;

    public TextChunkingService(KnowledgeChunkingProperties properties) {
        this.properties = properties;
        validateConfiguration();
    }

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Content must not be null or blank");
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int limit = Math.min(start + properties.getChunkSize(), text.length());
            int paragraph = limit < text.length() ? text.lastIndexOf("\n\n", limit) : -1;
            int sentence = limit < text.length() ? findLastSentenceBoundary(text, start, limit) : -1;
            int end = paragraph > start
                    ? paragraph + 2
                    : sentence > start
                            ? sentence
                            : limit == text.length()
                                    ? text.length()
                                    : findLastWhitespaceOrLimit(text, start, limit);

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end >= text.length()) {
                break;
            }

            boolean naturalBoundary = paragraph > start || sentence > start;
            int nextStart = naturalBoundary
                    ? end
                    : Math.max(start + 1, end - properties.getChunkOverlap());
            start = skipWhitespace(text, nextStart);
        }
        return chunks;
    }

    private int findLastSentenceBoundary(String text, int start, int end) {
        for (int index = end - 1; index >= start; index--) {
            char character = text.charAt(index);
            if ((character == '.' || character == '!' || character == '?')
                    && (index + 1 == text.length() || Character.isWhitespace(text.charAt(index + 1)))) {
                return index + 1;
            }
        }
        return -1;
    }

    private int findLastWhitespaceOrLimit(String text, int start, int end) {
        for (int index = end - 1; index > start; index--) {
            if (Character.isWhitespace(text.charAt(index))) {
                return index;
            }
        }
        return end;
    }

    private int skipWhitespace(String text, int start) {
        while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        return start;
    }

    private void validateConfiguration() {
        if (properties.getChunkSize() <= 0) {
            throw new IllegalArgumentException("knowledge.chunk-size must be greater than zero");
        }
        if (properties.getChunkOverlap() < 0
                || properties.getChunkOverlap() >= properties.getChunkSize()) {
            throw new IllegalArgumentException(
                    "knowledge.chunk-overlap must be between zero and chunk-size - 1");
        }
    }
}
