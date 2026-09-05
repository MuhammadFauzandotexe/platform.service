package mdro.platform.service.controller.whatsapp;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mdro.platform.service.dto.whatsapp.request.CreateWhatsAppSessionRequest;
import mdro.platform.service.dto.whatsapp.request.SynchronizeWhatsAppSessionRequest;
import mdro.platform.service.dto.whatsapp.response.WhatsAppSessionResponse;
import mdro.platform.service.dto.whatsapp.response.WhatsAppSessionSynchronizationResponse;
import mdro.platform.service.security.principal.AccountPrincipal;
import mdro.platform.service.service.whatsapp.WhatsAppSessionService;
import mdro.platform.service.service.whatsapp.WhatsAppSessionSynchronizationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/whatsapp/sessions")
@RequiredArgsConstructor
public class WhatsAppSessionController {

    private final WhatsAppSessionService sessionService;
    private final WhatsAppSessionSynchronizationService synchronizationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WhatsAppSessionResponse> createSession(
            @RequestBody CreateWhatsAppSessionRequest request,
            @AuthenticationPrincipal AccountPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionService.createSession(request, principal));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WhatsAppSessionResponse>> getAllSessions(
            @AuthenticationPrincipal AccountPrincipal principal) {
        return ResponseEntity.ok(sessionService.getAllSessions(principal));
    }

    @PostMapping("/sync")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WhatsAppSessionSynchronizationResponse> synchronizeSession(
            @RequestBody SynchronizeWhatsAppSessionRequest request,
            @AuthenticationPrincipal AccountPrincipal principal) {
        return ResponseEntity.ok(
                synchronizationService.synchronizeSession(request.sessionName(), principal));
    }
}
