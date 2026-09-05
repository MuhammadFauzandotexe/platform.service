package mdro.platform.service.security.principal;

import java.util.UUID;
import mdro.platform.service.model.account.AccountPlan;
import mdro.platform.service.model.account.AccountStatus;

public record AccountPrincipal(
        UUID accountId,
        String email,
        AccountStatus accountStatus,
        AccountPlan accountPlan
) {
}
