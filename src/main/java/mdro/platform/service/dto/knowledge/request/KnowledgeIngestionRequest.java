package mdro.platform.service.dto.knowledge.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record KnowledgeIngestionRequest(
        @NotNull(message = "tenantId is required")
        UUID tenantId,
        @NotNull(message = "documentId is required")
        UUID documentId,
        @NotBlank(message = "content must not be blank")
        @Size(max = 1_000_000, message = "content must not exceed 1,000,000 characters")
        String content) {
}
