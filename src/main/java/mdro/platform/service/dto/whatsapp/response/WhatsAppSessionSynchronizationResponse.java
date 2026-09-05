package mdro.platform.service.dto.whatsapp.response;

import java.util.UUID;
import mdro.platform.service.model.whatsapp.WhatsAppSessionStatus;

public record WhatsAppSessionSynchronizationResponse(
        String sessionName,
        UUID sessionId,
        WhatsAppSessionStatus status,
        boolean connected) {
}
