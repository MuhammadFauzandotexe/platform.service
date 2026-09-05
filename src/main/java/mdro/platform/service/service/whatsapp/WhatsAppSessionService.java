package mdro.platform.service.service.whatsapp;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mdro.platform.service.config.WhatsAppSessionConfiguration;
import mdro.platform.service.dto.whatsapp.request.CreateWhatsAppSessionRequest;
import mdro.platform.service.dto.whatsapp.response.WhatsAppSessionResponse;
import mdro.platform.service.entity.Account;
import mdro.platform.service.entity.WhatsAppSession;
import mdro.platform.service.integration.whatsapp.client.WhatsAppGatewayClient;
import mdro.platform.service.exception.whatsapp.WhatsAppSessionAccountNotFoundException;
import mdro.platform.service.repository.AccountRepository;
import mdro.platform.service.repository.WhatsAppSessionRepository;
import mdro.platform.service.security.principal.AccountPrincipal;
import mdro.platform.service.model.whatsapp.WhatsAppSessionStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WhatsAppSessionService {

    private static final Duration QR_EXPIRATION = Duration.ofSeconds(60);

    private final AccountRepository accountRepository;
    private final WhatsAppSessionRepository sessionRepository;
    private final WhatsAppGatewayClient gatewayClient;
    private final WhatsAppSessionConfiguration configuration;

    public WhatsAppSessionResponse createSession(
            CreateWhatsAppSessionRequest request,
            AccountPrincipal principal) {
        Account account = accountRepository.findById(principal.accountId())
                .orElseThrow(() -> new WhatsAppSessionAccountNotFoundException(principal.accountId()));

        UUID sessionId = UUID.randomUUID();
        WhatsAppSession session = sessionRepository.save(
                new WhatsAppSession(sessionId, request.sessionName(), account));

        try {
            mdro.platform.service.integration.whatsapp.dto.WhatsAppSessionResponse gatewayResponse =
                    gatewayClient.createSession(account.getId(), sessionId);
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            WhatsAppSessionStatus status = WhatsAppSessionStatus.valueOf(gatewayResponse.status());
            OffsetDateTime expiresAt = status == WhatsAppSessionStatus.QR_READY
                    ? now.plus(configuration.getPendingExpiration())
                    : null;
            OffsetDateTime connectedAt = status == WhatsAppSessionStatus.CONNECTED ? now : null;
            session.markGatewayResponse(status, expiresAt, connectedAt);
            sessionRepository.save(session);
            return toResponse(session, gatewayResponse.qrCode(),
                    status == WhatsAppSessionStatus.QR_READY ? now.plus(QR_EXPIRATION) : null);
        } catch (RuntimeException exception) {
            session.markFailed();
            sessionRepository.save(session);
            throw exception;
        }
    }

    private WhatsAppSessionResponse toResponse(
            WhatsAppSession session,
            String qrCode,
            OffsetDateTime qrExpiresAt) {
        return new WhatsAppSessionResponse(
                session.getId(),
                session.getSessionId(),
                session.getSessionName(),
                session.getStatus(),
                qrCode,
                qrExpiresAt,
                session.getExpiresAt());
    }
}
