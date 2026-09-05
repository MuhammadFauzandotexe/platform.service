package mdro.platform.service.integration.whatsapp.dto;

import java.util.UUID;

public record WhatsAppSessionResponse(
        UUID tenantId,
        UUID sessionId,
        String status,
        String qrCode) {
}
