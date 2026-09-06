package mdro.platform.service.security.tenant;

import java.util.UUID;
import mdro.platform.service.entity.Account;

public record AuthorizedTenantContext(
        UUID accountId,
        UUID tenantId,
        Account account) {
}
