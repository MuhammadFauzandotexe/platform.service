package mdro.platform.service.dto.account.response;

import java.util.UUID;
import mdro.platform.service.model.account.AccountPlan;
import mdro.platform.service.model.account.AccountStatus;

public record AccountRegistrationResponse(
        UUID id,
        String email,
        AccountStatus accountStatus,
        AccountPlan accountPlan,
        String message
) {
}
