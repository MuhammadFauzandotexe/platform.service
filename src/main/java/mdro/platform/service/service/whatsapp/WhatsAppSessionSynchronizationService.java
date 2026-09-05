package mdro.platform.service.service.whatsapp;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mdro.platform.service.dto.whatsapp.response.WhatsAppSessionSynchronizationResponse;
import mdro.platform.service.entity.WhatsAppSession;
import mdro.platform.service.integration.whatsapp.client.WhatsAppGatewayClient;
import mdro.platform.service.integration.whatsapp.dto.WhatsAppSessionResponse;
import mdro.platform.service.integration.whatsapp.exception.WhatsAppGatewayException;
import mdro.platform.service.exception.whatsapp.WhatsAppSessionNotFoundException;
import mdro.platform.service.model.whatsapp.WhatsAppSessionStatus;
import mdro.platform.service.repository.WhatsAppSessionRepository;
import mdro.platform.service.security.principal.AccountPrincipal;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppSessionSynchronizationService {

    private final WhatsAppSessionRepository sessionRepository;
    private final WhatsAppGatewayClient gatewayClient;

    public WhatsAppSessionSynchronizationResponse synchronizeSession(
            String sessionName,
            AccountPrincipal principal) {
        UUID accountId = principal.accountId();
        List<WhatsAppSession> candidates =
                sessionRepository.findAllByAccount_IdAndSessionName(accountId, sessionName);
        if (candidates.isEmpty()) {
            throw new WhatsAppSessionNotFoundException(sessionName);
        }

        log.info("WhatsApp session synchronization started: accountId={}, sessionName={}, candidates={}",
                accountId, sessionName, candidates.size());

        List<WhatsAppSessionResponse> gatewaySessions = gatewayClient.getSessionsByTenant(accountId);
        Map<UUID, WhatsAppSessionResponse> gatewayBySessionId = new HashMap<>();
        for (WhatsAppSessionResponse gatewaySession : gatewaySessions) {
            gatewayBySessionId.put(gatewaySession.sessionId(), gatewaySession);
        }

        WhatsAppSession connectedSession = null;
        int connectedCount = 0;
        WhatsAppSessionStatus resultStatus = null;
        UUID resultSessionId = null;
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        for (WhatsAppSession candidate : candidates) {
            WhatsAppSessionResponse gatewaySession = gatewayBySessionId.get(candidate.getSessionId());
            WhatsAppSessionStatus gatewayStatus = gatewaySession == null
                    ? candidate.getStatus()
                    : parseStatus(gatewaySession.status());

            log.debug("Checking WhatsApp Gateway session: accountId={}, sessionName={}, sessionId={}, status={}",
                    accountId, sessionName, candidate.getSessionId(), gatewayStatus);

            if (gatewayStatus == WhatsAppSessionStatus.CONNECTED) {
                connectedCount++;
                connectedSession = candidate;
                candidate.markConnected(now);
                sessionRepository.save(candidate);
                resultStatus = gatewayStatus;
                resultSessionId = candidate.getSessionId();
                log.info("Connected WhatsApp session detected: accountId={}, sessionName={}, sessionId={}",
                        accountId, sessionName, candidate.getSessionId());
            } else {
                updateNonConnectedStatus(candidate, gatewayStatus, now);
                if (resultStatus == null || resultStatus == WhatsAppSessionStatus.CREATING) {
                    resultStatus = gatewayStatus;
                    resultSessionId = candidate.getSessionId();
                }
            }
        }

        if (connectedCount == 1) {
            cleanupStaleSessions(accountId, sessionName, connectedSession.getId(),
                    connectedSession.getSessionId(), gatewayBySessionId);
        } else if (connectedCount > 1) {
            log.error("Multiple connected WhatsApp sessions detected; skipping cleanup: "
                            + "accountId={}, sessionName={}, count={}",
                    accountId, sessionName, connectedCount);
        }

        if (resultStatus == null) {
            resultStatus = candidates.get(0).getStatus();
            resultSessionId = candidates.get(0).getSessionId();
        }
        return new WhatsAppSessionSynchronizationResponse(
                sessionName,
                resultSessionId,
                resultStatus,
                connectedCount > 0);
    }

    public void cleanupStaleSessions(
            UUID accountId,
            String sessionName,
            UUID connectedDatabaseId,
            UUID connectedSessionId,
            Map<UUID, WhatsAppSessionResponse> gatewayBySessionId) {
        List<WhatsAppSession> staleSessions =
                sessionRepository.findAllByAccount_IdAndSessionNameAndIdNot(
                        accountId, sessionName, connectedDatabaseId);
        log.info("Stale WhatsApp sessions identified: accountId={}, sessionName={}, count={}",
                accountId, sessionName, staleSessions.size());

        for (WhatsAppSession staleSession : staleSessions) {
            if (!isTemporarySession(staleSession)) {
                continue;
            }
            if (staleSession.getSessionId().equals(connectedSessionId)) {
                continue;
            }

            WhatsAppSessionResponse gatewaySession = gatewayBySessionId.get(staleSession.getSessionId());
            if (gatewaySession != null && !WhatsAppSessionStatus.CONNECTED.name().equals(gatewaySession.status())) {
                log.info("Deleting stale WhatsApp Gateway session: accountId={}, sessionName={}, sessionId={}",
                        accountId, sessionName, staleSession.getSessionId());
                deleteGatewayIfPresent(accountId, staleSession.getSessionId());
            }
            sessionRepository.delete(staleSession);
        }
        log.info("Stale WhatsApp database cleanup completed: accountId={}, sessionName={}",
                accountId, sessionName);
    }

    private boolean isTemporarySession(WhatsAppSession session) {
        return session.getStatus() == WhatsAppSessionStatus.CREATING
                || session.getStatus() == WhatsAppSessionStatus.QR_READY;
    }

    private void updateNonConnectedStatus(
            WhatsAppSession session,
            WhatsAppSessionStatus gatewayStatus,
            OffsetDateTime now) {
        if (gatewayStatus == WhatsAppSessionStatus.QR_READY) {
            session.markQrReady();
            sessionRepository.save(session);
        } else if (gatewayStatus == WhatsAppSessionStatus.DISCONNECTED) {
            session.markDisconnected(now);
            sessionRepository.save(session);
        }
    }

    private void deleteGatewayIfPresent(UUID accountId, UUID sessionId) {
        try {
            gatewayClient.deleteSession(accountId, sessionId);
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
}
