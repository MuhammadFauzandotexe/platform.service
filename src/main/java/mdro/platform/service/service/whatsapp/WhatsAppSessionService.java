package mdro.platform.service.service.whatsapp;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mdro.platform.service.config.WhatsAppSessionConfiguration;
import mdro.platform.service.dto.whatsapp.request.CreateWhatsAppSessionRequest;
import mdro.platform.service.dto.whatsapp.response.WhatsAppSessionResponse;
import mdro.platform.service.entity.Account;
import mdro.platform.service.entity.WhatsAppSession;
import mdro.platform.service.integration.whatsapp.client.WhatsAppGatewayClient;
import mdro.platform.service.integration.whatsapp.exception.WhatsAppGatewayException;
import mdro.platform.service.exception.whatsapp.WhatsAppSessionAccountNotFoundException;
import mdro.platform.service.exception.whatsapp.WhatsAppSessionAlreadyExistsException;
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

        if (sessionRepository.existsByAccount_IdAndSessionNameAndStatus(
                account.getId(),
                request.sessionName(),
                WhatsAppSessionStatus.CONNECTED)) {
            throw new WhatsAppSessionAlreadyExistsException(request.sessionName());
        }

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

    public List<WhatsAppSessionResponse> getAllSessions(AccountPrincipal principal) {
        Account account = accountRepository.findById(principal.accountId())
                .orElseThrow(() -> new WhatsAppSessionAccountNotFoundException(principal.accountId()));

        List<WhatsAppSession> platformSessions = sessionRepository.findAllByAccount_Id(account.getId());
        List<mdro.platform.service.integration.whatsapp.dto.WhatsAppSessionResponse> gatewaySessions =
                gatewayClient.getSessionsByTenant(account.getId());
        Map<UUID, WhatsAppSession> platformBySessionId = new HashMap<>();
        for (WhatsAppSession session : platformSessions) {
            platformBySessionId.put(session.getSessionId(), session);
        }
        Map<UUID, mdro.platform.service.integration.whatsapp.dto.WhatsAppSessionResponse> gatewayBySessionId =
                new HashMap<>();
        for (mdro.platform.service.integration.whatsapp.dto.WhatsAppSessionResponse session : gatewaySessions) {
            gatewayBySessionId.put(session.sessionId(), session);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        List<WhatsAppSessionResponse> responses = new ArrayList<>();
        for (WhatsAppSession session : platformSessions) {
            SessionSynchronizationAction action = processSessionCleanupDecision(
                    session,
                    gatewayBySessionId.get(session.getSessionId()),
                    now);
            executeSynchronizationAction(session, gatewayBySessionId.get(session.getSessionId()), action, now);
            if (action != SessionSynchronizationAction.DELETE_GATEWAY_AND_DB) {
                responses.add(toResponse(session));
            }
        }
        return responses;
    }

    private SessionSynchronizationAction processSessionCleanupDecision(
            WhatsAppSession platformSession,
            mdro.platform.service.integration.whatsapp.dto.WhatsAppSessionResponse gatewaySession,
            OffsetDateTime now) {
        if (gatewaySession != null) {
            WhatsAppSessionStatus gatewayStatus = parseStatus(gatewaySession.status());
            return switch (gatewayStatus) {
                case CONNECTED -> SessionSynchronizationAction.UPDATE_TO_CONNECTED;
                case DISCONNECTED -> SessionSynchronizationAction.DELETE_GATEWAY_ONLY;
                case QR_READY -> isExpired(platformSession, now)
                        ? SessionSynchronizationAction.DELETE_GATEWAY_AND_DB
                        : SessionSynchronizationAction.UPDATE_TO_QR_READY;
                default -> SessionSynchronizationAction.KEEP;
            };
        }

        if (isExpired(platformSession, now)) {
            return SessionSynchronizationAction.DELETE_GATEWAY_AND_DB;
        }
        return platformSession.getStatus() == WhatsAppSessionStatus.CONNECTED
                ? SessionSynchronizationAction.UPDATE_TO_DISCONNECTED
                : SessionSynchronizationAction.KEEP;
    }

    private boolean isExpired(WhatsAppSession platformSession, OffsetDateTime now) {
        return platformSession.getStatus() != WhatsAppSessionStatus.CONNECTED
                && platformSession.getExpiresAt() != null
                && !platformSession.getExpiresAt().isAfter(now);
    }

    private void executeSynchronizationAction(
            WhatsAppSession platformSession,
            mdro.platform.service.integration.whatsapp.dto.WhatsAppSessionResponse gatewaySession,
            SessionSynchronizationAction action,
            OffsetDateTime now) {
        switch (action) {
            case DELETE_GATEWAY_AND_DB -> {
                deleteGatewayIfPresent(platformSession, gatewaySession);
                sessionRepository.delete(platformSession);
            }
            case DELETE_GATEWAY_ONLY -> {
                platformSession.markDisconnected(now);
                sessionRepository.save(platformSession);
                deleteGateway(platformSession);
            }
            case UPDATE_TO_DISCONNECTED -> {
                platformSession.markDisconnected(now);
                sessionRepository.save(platformSession);
            }
            case UPDATE_TO_CONNECTED -> {
                platformSession.markConnected(now);
                sessionRepository.save(platformSession);
            }
            case UPDATE_TO_QR_READY -> {
                platformSession.markQrReady();
                sessionRepository.save(platformSession);
            }
            case KEEP -> {
            }
        }
    }

    private void deleteGatewayIfPresent(
            WhatsAppSession platformSession,
            mdro.platform.service.integration.whatsapp.dto.WhatsAppSessionResponse gatewaySession) {
        if (gatewaySession != null) {
            deleteGateway(platformSession);
        }
    }

    private void deleteGateway(WhatsAppSession platformSession) {
        try {
            gatewayClient.deleteSession(platformSession.getAccount().getId(), platformSession.getSessionId());
        } catch (WhatsAppGatewayException exception) {
            if (!"SESSION_NOT_FOUND".equals(exception.getGatewayError())) {
                throw exception;
            }
        }
    }

    private WhatsAppSessionStatus parseStatus(String status) {
        try {
            return WhatsAppSessionStatus.valueOf(status);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Unexpected WhatsApp Gateway session status: " + status, exception);
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
                session.getExpiresAt(),
                null,
                session.getConnectedAt(),
                session.getDisconnectedAt(),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }

    private WhatsAppSessionResponse toResponse(WhatsAppSession session) {
        return toResponse(session, null, null);
    }

    private enum SessionSynchronizationAction {
        DELETE_GATEWAY_AND_DB,
        DELETE_GATEWAY_ONLY,
        UPDATE_TO_DISCONNECTED,
        UPDATE_TO_CONNECTED,
        UPDATE_TO_QR_READY,
        KEEP
    }
}
