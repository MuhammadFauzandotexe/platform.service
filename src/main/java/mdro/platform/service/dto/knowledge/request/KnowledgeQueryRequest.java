package mdro.platform.service.dto.knowledge.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KnowledgeQueryRequest(
        @NotBlank(message = "question must not be blank")
        @Size(min = 3, max = 2000, message = "question must be between 3 and 2000 characters")
        String question) {
}
