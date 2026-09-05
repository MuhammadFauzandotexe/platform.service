package mdro.platform.service.service.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import mdro.platform.service.config.WhatsAppSessionConfiguration;
import mdro.platform.service.entity.Account;
import mdro.platform.service.entity.WhatsAppSession;
import mdro.platform.service.integration.whatsapp.client.WhatsAppGatewayClient;
import mdro.platform.service.integration.whatsapp.dto.WhatsAppSessionResponse;
import mdro.platform.service.model.account.AccountPlan;
import mdro.platform.service.model.account.AccountStatus;
import mdro.platform.service.model.whatsapp.WhatsAppSessionStatus;
import mdro.platform.service.repository.AccountRepository;
import mdro.platform.service.repository.WhatsAppSessionRepository;
import mdro.platform.service.security.principal.AccountPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WhatsAppSessionServiceSynchronizationTest {

    private final UUID accountId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final Account account = org.mockito.Mockito.mock(Account.class);
    private final AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
    private final WhatsAppSessionRepository sessionRepository =
            org.mockito.Mockito.mock(WhatsAppSessionRepository.class);
    private final WhatsAppGatewayClient gatewayClient =
            org.mockito.Mockito.mock(WhatsAppGatewayClient.class);
    private final WhatsAppSessionConfiguration configuration = new WhatsAppSessionConfiguration();

    private WhatsAppSessionService service;
    private AccountPrincipal principal;

    @BeforeEach
    void setUp() {
        when(account.getId()).thenReturn(accountId);
        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(account));
        principal = new AccountPrincipal(
                accountId,
                "user@example.com",
                AccountStatus.REGISTERED,
                AccountPlan.FREE);
        service = new WhatsAppSessionService(
                accountRepository,
                sessionRepository,
                gatewayClient,
                configuration);
    }

    @Test
    void connectedGatewaySessionOverridesExpiredPlatformSession() {
        WhatsAppSession session = session(WhatsAppSessionStatus.QR_READY, OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        when(sessionRepository.findAllByAccount_Id(accountId)).thenReturn(List.of(session));
        when(gatewayClient.getSessionsByTenant(accountId)).thenReturn(List.of(
                gatewaySession(WhatsAppSessionStatus.CONNECTED)));

        List<mdro.platform.service.dto.whatsapp.response.WhatsAppSessionResponse> responses =
                service.getAllSessions(principal);

        assertEquals(WhatsAppSessionStatus.CONNECTED, session.getStatus());
        assertNull(session.getExpiresAt());
        assertEquals(1, responses.size());
        verify(gatewayClient, never()).deleteSession(accountId, sessionId);
        verify(sessionRepository, never()).delete(session);
    }

    @Test
    void expiredQrReadySessionIsDeletedFromGatewayAndDatabase() {
        WhatsAppSession session = session(WhatsAppSessionStatus.QR_READY, OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        when(sessionRepository.findAllByAccount_Id(accountId)).thenReturn(List.of(session));
        when(gatewayClient.getSessionsByTenant(accountId)).thenReturn(List.of(
                gatewaySession(WhatsAppSessionStatus.QR_READY)));

        List<mdro.platform.service.dto.whatsapp.response.WhatsAppSessionResponse> responses =
                service.getAllSessions(principal);

        assertEquals(0, responses.size());
        verify(gatewayClient).deleteSession(accountId, sessionId);
        verify(sessionRepository).delete(session);
    }

    @Test
    void disconnectedGatewaySessionIsKeptInDatabaseAndDeletedFromGateway() {
        WhatsAppSession session = session(WhatsAppSessionStatus.CONNECTED, null);
        when(sessionRepository.findAllByAccount_Id(accountId)).thenReturn(List.of(session));
        when(gatewayClient.getSessionsByTenant(accountId)).thenReturn(List.of(
                gatewaySession(WhatsAppSessionStatus.DISCONNECTED)));

        service.getAllSessions(principal);

        assertEquals(WhatsAppSessionStatus.DISCONNECTED, session.getStatus());
        verify(sessionRepository).save(session);
        verify(gatewayClient).deleteSession(accountId, sessionId);
        verify(sessionRepository, never()).delete(session);
    }

    @Test
    void connectedPlatformSessionMissingFromGatewayBecomesDisconnected() {
        WhatsAppSession session = session(WhatsAppSessionStatus.CONNECTED, null);
        when(sessionRepository.findAllByAccount_Id(accountId)).thenReturn(List.of(session));
        when(gatewayClient.getSessionsByTenant(accountId)).thenReturn(List.of());

        service.getAllSessions(principal);

        assertEquals(WhatsAppSessionStatus.DISCONNECTED, session.getStatus());
        verify(sessionRepository).save(session);
        verify(gatewayClient, never()).deleteSession(any(), any());
        verify(sessionRepository, never()).delete(session);
    }

    @Test
    void disconnectedPlatformSessionMissingFromGatewayIsKept() {
        WhatsAppSession session = session(WhatsAppSessionStatus.DISCONNECTED, null);
        when(sessionRepository.findAllByAccount_Id(accountId)).thenReturn(List.of(session));
        when(gatewayClient.getSessionsByTenant(accountId)).thenReturn(List.of());

        service.getAllSessions(principal);

        assertEquals(WhatsAppSessionStatus.DISCONNECTED, session.getStatus());
        verify(sessionRepository, never()).save(session);
        verify(gatewayClient, never()).deleteSession(any(), any());
        verify(sessionRepository, never()).delete(session);
    }

    private WhatsAppSession session(WhatsAppSessionStatus status, OffsetDateTime expiresAt) {
        WhatsAppSession session = new WhatsAppSession(sessionId, "Support", account);
        if (status == WhatsAppSessionStatus.CONNECTED) {
            session.markConnected(OffsetDateTime.now(ZoneOffset.UTC));
        } else if (status == WhatsAppSessionStatus.DISCONNECTED) {
            session.markDisconnected(OffsetDateTime.now(ZoneOffset.UTC));
        } else if (status == WhatsAppSessionStatus.QR_READY) {
            session.markGatewayResponse(status, expiresAt, null);
        }
        return session;
    }

    private WhatsAppSessionResponse gatewaySession(WhatsAppSessionStatus status) {
        return new WhatsAppSessionResponse(accountId, sessionId, status.name(), null);
    }
}
