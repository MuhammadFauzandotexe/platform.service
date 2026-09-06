package mdro.platform.service.security.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import mdro.platform.service.entity.Account;
import mdro.platform.service.model.account.AccountPlan;
import mdro.platform.service.model.account.AccountStatus;
import mdro.platform.service.repository.AccountRepository;
import mdro.platform.service.security.principal.AccountPrincipal;
import org.junit.jupiter.api.Test;

class TenantContextResolverTest {

    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final TenantContextResolver resolver = new TenantContextResolver(accountRepository);
    private final UUID accountId = UUID.randomUUID();

    @Test
    void resolvesTenantFromDatabaseAccountAndIgnoresPrincipalTenantClaims() {
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(accountId);
        when(account.getAccountStatus()).thenReturn(AccountStatus.ACCOUNT_VERIFIED);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        AuthorizedTenantContext context = resolver.resolveCurrentTenant(new AccountPrincipal(
                accountId,
                "user@example.com",
                AccountStatus.ACCOUNT_VERIFIED,
                AccountPlan.FREE));

        assertEquals(accountId, context.accountId());
        assertEquals(accountId, context.tenantId());
        assertEquals(account, context.account());
    }

    @Test
    void rejectsWhenAuthenticatedAccountNoLongerExists() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(
                TenantAuthorizationException.class,
                () -> resolver.resolveCurrentTenant(new AccountPrincipal(
                        accountId,
                        "user@example.com",
                        AccountStatus.ACCOUNT_VERIFIED,
                        AccountPlan.FREE)));
    }

    @Test
    void rejectsSuspendedAccount() {
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(accountId);
        when(account.getAccountStatus()).thenReturn(AccountStatus.SUSPENDED);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThrows(
                TenantAuthorizationException.class,
                () -> resolver.resolveCurrentTenant(new AccountPrincipal(
                        accountId,
                        "user@example.com",
                        AccountStatus.ACCOUNT_VERIFIED,
                        AccountPlan.FREE)));
    }
}
