package mdro.platform.service.dto.account.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountInfoResponse(
        String accountStatus,
        OffsetDateTime createdAt,
        String accountPlan,
        UUID id,
        String email,
        OffsetDateTime updatedAt
) {
}
