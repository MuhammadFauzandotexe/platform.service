package mdro.platform.service.dto.whatsapp.response;

import java.time.OffsetDateTime;
import java.util.UUID;
import mdro.platform.service.model.whatsapp.WhatsAppSessionStatus;

public record WhatsAppSessionResponse(
        UUID id,
        UUID sessionId,
        String sessionName,
        WhatsAppSessionStatus status,
        String qrCode,
        OffsetDateTime qrExpiresAt,
        OffsetDateTime expiresAt) {
}
