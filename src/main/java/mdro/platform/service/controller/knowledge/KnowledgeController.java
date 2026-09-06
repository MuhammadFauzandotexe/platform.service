package mdro.platform.service.controller.knowledge;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mdro.platform.service.dto.knowledge.request.KnowledgeIngestionRequest;
import mdro.platform.service.dto.knowledge.response.KnowledgeIngestionResponse;
import mdro.platform.service.service.knowledge.KnowledgeIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeIngestionService knowledgeIngestionService;

    @PostMapping("/ingest")
    public ResponseEntity<KnowledgeIngestionResponse> ingest(
            @Valid @RequestBody KnowledgeIngestionRequest request) {
        return ResponseEntity.ok(knowledgeIngestionService.ingest(request));
    }
}
