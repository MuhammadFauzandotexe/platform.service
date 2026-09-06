package mdro.platform.service.security.tenant;

import lombok.RequiredArgsConstructor;
import mdro.platform.service.entity.Account;
import mdro.platform.service.model.account.AccountStatus;
import mdro.platform.service.repository.AccountRepository;
import mdro.platform.service.security.principal.AccountPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantContextResolver {

    private final AccountRepository accountRepository;

    public AuthorizedTenantContext resolveCurrentTenant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw new TenantAuthorizationException("Authenticated account could not be resolved");
        }
        return resolveCurrentTenant(principal);
    }

    public AuthorizedTenantContext resolveCurrentTenant(AccountPrincipal principal) {
        if (principal == null || principal.accountId() == null) {
            throw new TenantAuthorizationException("Authenticated account could not be resolved");
        }

        Account account = accountRepository.findById(principal.accountId())
                .orElseThrow(() -> new TenantAuthorizationException(
                        "Authenticated account is not available"));

        if (account.getAccountStatus() == AccountStatus.SUSPENDED
                || account.getAccountStatus() == AccountStatus.DISABLED) {
            throw new TenantAuthorizationException("Authenticated account is not allowed to use this resource");
        }

        // The current schema has no separate Tenant entity; account identity is the tenant boundary.
        return new AuthorizedTenantContext(account.getId(), account.getId(), account);
    }
}
