package mdro.platform.service.dto.auth.response;

import java.util.UUID;
import mdro.platform.service.model.account.AccountPlan;
import mdro.platform.service.model.account.AccountStatus;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthenticatedAccount account
) {
    public record AuthenticatedAccount(
            UUID id,
            String email,
            AccountStatus accountStatus,
            AccountPlan accountPlan
    ) {
    }
}
