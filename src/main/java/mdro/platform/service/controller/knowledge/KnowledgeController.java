package mdro.platform.service.controller.knowledge;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mdro.platform.service.dto.knowledge.request.KnowledgeIngestionRequest;
import mdro.platform.service.dto.knowledge.response.KnowledgeIngestionResponse;
import mdro.platform.service.dto.knowledge.request.KnowledgeQueryRequest;
import mdro.platform.service.dto.knowledge.response.KnowledgeQueryResponse;
import mdro.platform.service.security.principal.AccountPrincipal;
import mdro.platform.service.security.tenant.TenantContextResolver;
import mdro.platform.service.service.knowledge.KnowledgeIngestionService;
import mdro.platform.service.service.knowledge.KnowledgeQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final KnowledgeQueryService knowledgeQueryService;
    private final TenantContextResolver tenantContextResolver;

    @PostMapping("/ingest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<KnowledgeIngestionResponse> ingest(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody KnowledgeIngestionRequest request) {
        return ResponseEntity.ok(knowledgeIngestionService.ingest(
                tenantContextResolver.resolveCurrentTenant(principal),
                request));
    }

    @PostMapping("/query")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<KnowledgeQueryResponse> query(
            @Valid @RequestBody KnowledgeQueryRequest request) {
        return ResponseEntity.ok(knowledgeQueryService.query(request));
    }
}
