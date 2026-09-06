package mdro.platform.service.dto.knowledge.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KnowledgeIngestionRequest(
        @NotNull(message = "documentId is required")
        UUID documentId,
        @NotBlank(message = "content must not be blank")
        @Size(max = 1_000_000, message = "content must not exceed 1,000,000 characters")
        String content) {
}
