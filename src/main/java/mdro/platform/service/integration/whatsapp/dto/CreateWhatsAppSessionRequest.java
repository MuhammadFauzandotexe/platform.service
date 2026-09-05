package mdro.platform.service.integration.whatsapp.dto;

import java.util.UUID;

public record CreateWhatsAppSessionRequest(UUID tenantId, UUID sessionId) {
}
